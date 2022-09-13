### 实现一个简易的retrofit

#### 使用到的主要知识点
- 泛型
- 注解
- 动态代理
- 建造者模式

#### 大致思路
- `EasyRetrofit`作为入口类，create方法代理了对应的接口，动态代理内部使用`ServiceMethod`来解析和存储每个请求的相关信息，然后调用invoke通过okhttp发出去

#### 实现了什么
- 调用方写法类似retrofit，目前只实现了get

#### 没有实现什么
- post等其他method的支持
- ServiceMethod的缓存
- callFactory可自定义
- 自定义序列化与反序列化的支持