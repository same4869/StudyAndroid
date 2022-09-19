## 全埋点方案
> 文章思路均出自于《Android全埋点解决方案》（2019-03），主要是提供了一些思路和实践可行性，基本囊括了大部分的相关知识点，对于投入实际使用还是有较远的距离。

> 主要对pv（$AppViewScreen）事件，launch（$AppStart和$AppEnd）事件，click（$AppClick）事件进行采集，其中click事件会使用较多篇幅和方案


### pv事件采集

- 技术点：Application.ActivityLifecycleCallbacks

#### 思路
- 在应用程序自定义的Application类的onCreate()方法中初始化埋点SDK，并传入当前的Application对象。埋点SDK拿到Application对象之后，通过调用Application的registerActivityLifecycleCallback(ActivityLifecycleCallbacks callback)方法注册Application.ActivityLifecycleCallbacks回调。这样埋点SDK就能对当前应用程序中所有的Activity的生命周期事件进行集中处理（监控）了。在注册的Application.ActivityLifecycleCallbacks的onActivityResumed(Activity activity)回调方法中，我们可以拿到当前正在显示的Activity对象，然后调用SDK的相关接口触发页面浏览事件（$AppViewScreen）即可。
- 项目见`AutoTrackAppViewScreen`

### launch事件采集
- 技术点：Application.ActivityLifecycleCallbacks，ContentProvider，计时器

#### 思路
- 总体来说，我们首先注册一个Application.ActivityLifecycleCallbacks回调，用来监听应用程序内所有Activity的生命周期。然后我们再分两种情况分别进行处理。在页面退出的时候（即onPause生命周期函数），我们会启动一个30s的倒计时，如果30s之内没有新的页面进来（或显示），则触发$AppEnd事件；如果有新的页面进来（或显示），则存储一个标记位来标记已有新的页面进来。这里需要注意的是，由于Activity之间可能是跨进程的（即给Activity设置了android：process属性），所以标记位需要实现进程间的共享，即通过ContentProvider+SharedPreferences来进行存储。然后通过ContentObserver监听到新页面进来的标记位改变，从而可以取消上个页面退出时启动的倒计时。如果30s之内没有新的页面进来（比如用户按Home键/返回键退出应用程序、应用程序发生崩溃、应用程序被强杀），则会触发$AppEnd事件，或者在下次启动的时候补发一个$AppEnd事件。之所以要补发$AppEnd事件，是因为对于一些特殊的情况（应用程序发生崩溃、应用程序被强杀），应用程序可能停止运行了，导致我们无法及时触发$AppEnd事件，只能在用户下次启动应用程序的时候进行补发。当然，如果用户再也不去启动应用程序或者将应用程序卸载，就会导致“丢失”$AppEnd事件。在页面启动的时候（即onStart生命周期函数），我们需要判断一下与上个页面的退出时间间隔是否超过了30s，如果没有超过30s，则直接触发$AppViewScreen事件。如果已超过了30s，我们则需要判断之前是否已经触发了$AppEnd事件（因为如果App崩溃了或者被强杀了，可能没有触发$AppEnd事件），如果没有，则先触发$AppEnd事件，然后再触发$AppStart和$AppViewScreen事件。
- 项目见`AutoTrackAppStartAppEnd`

#### 缺点
- 应用程序发生崩溃或者应用程序被强杀等场景，需要下次启动应用程序的时候才能有机会补发$AppEnd事件。如果用户不再启动应用程序或者将应用程序卸载掉，会导致“丢失”$AppEnd事件。

### Click事件采集

#### 方案1
- 技术点：Application.ActivityLifecycleCallbacks，ViewTreeObserver.OnGlobalLayoutListener，反射

#### 思路
- 在应用程序自定义的Application对象的onCreate()方法中初始化埋点SDK，并传入当前Application对象。埋点SDK拿到Application对象之后，就可以通过调用registerActivityLifecycleCallback方法来注册Application.ActivityLifecycleCallbacks回调。这样埋点SDK就能对当前应用程序中所有Activity的生命周期事件进行集中处理（监控）了。在Application.ActivityLifecycleCallbacks的onActivityResumed(Activity activity)回调方法中，我们可以拿到当前正在显示的Activity实例，通过activity.findViewById(android.R.id.content)方法就可以拿到整个内容区域对应的View（是一个FrameLayout）。本书有可能会用RootView、ViewTree和根视图概念来混称这个View。然后，埋点SDK再逐层遍历这个RootView，并判断当前View是否设置了mOnClickListener对象，如果已设置mOnClickListener对象并且mOnClickListener又不是我们自定义的WrapperOnClickListener类型，则通过WrapperOnClickListener代理当前View设置的mOnClickListener。WrapperOnClickListener是我们自定义的一个类，它实现了View.OnClickListener接口，在WrapperOnClickListener的onClick方法里会先调用View的原有mOnClickListener处理逻辑，然后再调用埋点代码，即可实现“插入”埋点代码，从而达到自动埋点的效果。
- 项目见`AutoTrackAppClick1`

#### 缺点
- 由于使用反射，效率比较低，对App的整体性能有一定的影响，也可能会引入兼容性方面的风险；
- 无法直接支持采集游离于Activity之上的View的点击，比如Dialog、Popup-Window等。

#### 方案2
- 技术点：代理Window.Callback

#### 思路
- Application.ActivityLifecycleCallbacks的onActivityCreated(Activity activity，Bundle bundle)回调方法中，我们可以拿到当前正在显示的Activity对象，通过activity.getWindow()方法可以拿到这个Activity对应的Window对象，再通过window.getCallback()方法就可以拿到当前对应的Window.Callback对象，最后通过自定义的WrapperWindowCallback代理这个Window.Callback对象。然后，在WrapperWindowCallback的dispatchTouchEvent(MotionEvent event)方法中通过MotionEvent参数找到那个被点击的View对象，并插入埋点代码，最后再调用原有Window.Callback的dispatchTouchEvent(MotionEvent event)方法，即可达到“插入”埋点代码的效果。
-  项目见`AutoTrackAppClick2`

#### 缺点
- 由于每次点击时，都需要去遍历一次RootView，所以效率相对来说比较低，对应用程序的整体性能影响也比较大。
- 无法采集像Dialog、PopupWindow等游离于Activity之外的控件的点击事件。

#### 方案3
- 技术点：代理View.AccessibilityDelegate

#### 思路
-在ActivityLifecycleCallbacks的onActivityResumed(Activity activity，Bundle bundle)回调方法中，我们可以拿到当前正在显示的Activity对象，然后再通过activity.getWindow().getDecorView()方法或者activity.findViewById(android.R.id.content)方法拿到当前Activity的RootView，通过rootView.getViewTreeObserver()方法可以拿到RootView的ViewTreeObserver对象，然后再通过addOnGlobalLayoutListener()方法给RootView注册ViewTreeObserver.OnGlobalLayoutListener监听器，这样我们就可以在收到当前Activity的视图状态发生改变时去主动遍历一次RootView，并用我们自定义的Sen-sorsDataAccessibilityDelegate代理当前View的mAccessibilityDelegate对象。在我们自定义的SensorsDataAccessibilityDelegate类中的sendAccessibilityEvent(View host，int eventType)方法实现里，我们先调用原有的mAccessibilityDelegate对象的sendAccessibilityEvent方法，然后再插入埋点代码，其中host即是当前被点击的View对象，从而可以做到自动埋点的效果。
-  项目见`AutoTrackAppClick3`

#### 缺点
- 由于使用反射，效率相对来说比较低，可能会引入兼容性方面问题的风险；
- 无法采集Dialog、PopupWindow等游离于Activity之外的控件的点击事件；
- 辅助功能需要用户手动开启，在部分Android ROM上辅助功能可能会失效。

#### 方案4
- 技术点：创建透明层

#### 思路
- 结合Android系统的事件处理机制，我们可以自定义一个透明的View，然后添加到每个Activity的最上层（面）。这样，每当用户点击任何控件时，直接点击的其实就是我们这个自定义的透明View。然后我们再重写View的onTouchEvent(MotionEvent event)方法，在return super.onTouchEvent(event)之前，就可以根据MontionEvent里的点击坐标信息（x，y），在当前Activity的RootView里找到实际上被点击的那个View对象。找到被点击的View之后，我们再通过自定义的WrapperOnClickListener代理当前View的mOnClickListener对象。自定义的WrapperOnClickListener类实际上实现了View.OnClickListener接口，在WrapperOnClickListener的onClick(View view)方法里会先调用View的原有mOnClickListener的onClick(View view)处理逻辑，然后再插入埋点代码，就能达到自动埋点效果了。
-  项目见`AutoTrackAppClick4`

#### 缺点
- 无法采集Dialog、PopupWindow的点击事件；
- 每次点击都需要遍历一次RootView，效率比较低。

#### 方案5
- 技术点：AOP，AspectJ，自定义插件

#### 思路
- 对于Android系统中的View，它的点击处理逻辑，都是通过设置相应的listener对象并重写相应的回调方法实现的。比如，对于Button、ImageView等控件，它设置的listener对象均是android.view.View.OnClickListener类型，然后重写它的onClick(android.view.View)回调方法。我们只要利用一定的技术原理，在应用程序编译期间（比如生成.dex之前），在其onClick(android.view.View)方法中插入相应的埋点代码，即可做到自动埋点，也就是全埋点。我们可以把AspectJ的处理脚本放到我们自定义的插件里，然后编写相应的切面类，再定义合适的PointCut用来匹配我们的织入目标方法（listener对象的相应回调方法），比如android.view.View.OnClickListener的onClick(android.view.View)方法，就可以在编译期间插入埋点代码，从而达到自动埋点即全埋点的效果。
-  项目见`AutoTrackAppClick5`
-  自定义插件项目见`AutoTrackAspectJProject1`

#### 缺点
- 无法织入第三方的库；
- 由于定义的切点依赖编程语言，目前该方案无法兼容Lambda语法；
- 会有一些兼容性方面的问题，比如：D8、Gradle 4.x等。

#### 方案6
- 技术点：自定义插件，Transform，ASM

#### 思路
- 我们可以自定义一个Gradle Plugin，然后注册一个Transform对象。在transform方法里，可以分别遍历目录和jar包，然后我们就可以遍历当前应用程序所有的.class文件。然后再利用ASM框架的相关API，去加载相应的.class文件、解析.class文件，就可以找到满足特定条件的.class文件和相关方法，最后去修改相应的方法以动态插入埋点字节码，从而达到自动埋点的效果。
- Tansform的例子项目见`AutoTrackTransformProject`
- 项目见`AutoTrackAppClick6`

#### 缺点
- 暂无

#### 方案7
- 技术点：Javassist，自定义插件，Transform

#### 思路
- 在自定义的plugin里，我们可以注册一个自定义的Transform，从而可以分别对当前应用程序的所有源码目录和jar包进行遍历。在遍历过程中，利用Javassist框架的API可以对满足特定条件的方法进行修改，比如插入相关埋点代码。整个原理与使用ASM框架类似，此时只是把操作.class文件的框架由ASM换成Javassist了。
- 项目见`AutoTrackAppClick7`

#### 缺点
- 暂无

#### 方案8
- 技术点：AST，APT

##### 思路
- 在自定义注解处理器的process方法里，通过roundEnvironment.getRootElements()方法可以拿到所有的Element对象，通过trees.getTree(element)方法可以拿到对应的抽象语法树（AST），然后我们自定义一个TreeTranslator，在visitMethodDef里即可对方法进行判断。如果是目标处理方法，则通过AST框架的相关API即可插入埋点代码，从而实现全埋点的效果。
-  项目见`AutoTrackAppClick8`（项目没跑起来）

#### 缺点
- com.sun.tools.javac.tree相关API语法晦涩，理解难度大，要求有一定的编译原理基础；
- APT无法扫描其他module，导致AST无法处理其他module；
- 不支持Lambda语法；·带有返回值的方法，很难把埋点代码插入到方法之后。