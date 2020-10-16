## 622第三周记录

### 单服务器高性能模式：Reactor与Proactor

#### Reactor

- 单Reactor单线程

  client -> Reactor -> Acceptor -> Dispatcher -> handler -> Processor

- 单Reactor多线程

  client -> Reactor -> Acceptor -> Dispatcher -> handler（异步线程） -> Processor

- 多Reactor多线程

  - Netty
  - Nginx

> Reactor：当我们去餐厅点餐时，付钱后服务员会给我们一个号牌，然后我们就会拿着这个号牌去座位等着，等到服务员叫我们的号牌时，我们再去取餐。
>
> Proactor：当我们去餐厅点餐时，付钱后服务员会给我们一个号牌，然后我们就会拿着这个号牌去座位等着，当餐准备好时，服务员会把餐送到我们的桌子上。