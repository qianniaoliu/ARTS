# Netty学习笔记

## ServerBootStrap启动流程

> Tips：代码行后面的数字对应下面中文解释的数字

通过一个简单的示例演示netty server端启动

```java
public static void main(String[] args) throws Exception {
    EventLoopGroup bossGroup = new NioEventLoopGroup(1);// 1
    EventLoopGroup workerGroup = new NioEventLoopGroup();// 2
    final EchoServerHandler serverHandler = new EchoServerHandler();
    try {
        ServerBootstrap b = new ServerBootstrap();// 3
        b.group(bossGroup, workerGroup) //4
            .channel(NioServerSocketChannel.class)//5
            .option(ChannelOption.SO_BACKLOG, 100)//6
            .handler(new LoggingHandler(LogLevel.INFO)) //7
            .childHandler(new ChannelInitializer<SocketChannel>() {//8
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ChannelPipeline p = ch.pipeline();
                    p.addLast(serverHandler);//9
                }
            });
        ChannelFuture f = b.bind(PORT).sync();//10
        // Wait until the server socket is closed.
        f.channel().closeFuture().sync();
    } finally {
        // Shut down all event loops to terminate all threads.
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}
```

> 1：定义接收客户端连接的线程池
>
> 2：定义处理客户端请求的线程池
>
> 3：实例化一个server端启动引导类
>
> 4：将boss线程与work线程添加到ServerBootstrap中
>
> 5：定义server端的channel类型为NioServerSocketChannel
>
> 6：定义Socket为非阻塞
>
> 7：NioServerSocketChannel的ChannelPipeline成员添加LoggingHandler处理器
>
> 8：NioSocketChannel的ChannelPipeline成员添加ChannelInitializer处理器，当Server端接收到Client连接时，会初始化一个SocketChannel，然后就会回调ChannelInitializer的initChannel方法
>
> 9：往NioSocketChannel的ChannelPipeline成员添加业务Handler处理器
>
> 10：server绑定端口启动

接下来详细分析一下server端是如何启动的，启动过程中会初始化哪些信息。先看一下`AbstractBootStrap#doBind(final SocketAddress localAddress)`过程

```java
private ChannelFuture doBind(final SocketAddress localAddress) {
    final ChannelFuture regFuture = initAndRegister();//1
    final Channel channel = regFuture.channel();
    if (regFuture.cause() != null) {
        return regFuture;
    }

    if (regFuture.isDone()) {//2
        ChannelPromise promise = channel.newPromise();
        doBind0(regFuture, channel, localAddress, promise);//3
        return promise;
    } else {
        // Registration future is almost always fulfilled already, but just in case it's not.
        final PendingRegistrationPromise promise = new PendingRegistrationPromise(channel);
        regFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                Throwable cause = future.cause();
                if (cause != null) {
                    promise.setFailure(cause);
                } else {
                    promise.registered();

                    doBind0(regFuture, channel, localAddress, promise);
                }
            }
        });
        return promise;
    }
}
```

> 1：实例化与初始化Channel信息，并将ServerSocketChannel注册到Selector选择器中
>
> 2：判断channel是否注册完成
>
> 3：调用ServerSocket.bind()底层方法绑定

注册与初始化channel：`AbstractBootstrap#initAndRegister()`

```java
final ChannelFuture initAndRegister() {
    Channel channel = null;
    try {
        channel = channelFactory.newChannel(); //1
        init(channel);//2
    } catch (Throwable t) {
        if (channel != null) {
            channel.unsafe().closeForcibly();
            return new DefaultChannelPromise(channel, GlobalEventExecutor.INSTANCE).setFailure(t);
        }
        return new DefaultChannelPromise(new FailedChannel(), GlobalEventExecutor.INSTANCE).setFailure(t);
    }

    ChannelFuture regFuture = config().group().register(channel);//15
    //...省略
    return regFuture;
}

private static final SelectorProvider DEFAULT_SELECTOR_PROVIDER = SelectorProvider.provider();//3

public NioServerSocketChannel() {
    this(newSocket(DEFAULT_SELECTOR_PROVIDER));//4
}

private static ServerSocketChannel newSocket(SelectorProvider provider) {
    try {
        return provider.openServerSocketChannel();//5
    } catch (IOException e) {
        throw new ChannelException(
            "Failed to open a server socket.", e);
    }
}

public NioServerSocketChannel(ServerSocketChannel channel) {
    super(null, channel, SelectionKey.OP_ACCEPT);//6
    config = new NioServerSocketChannelConfig(this, javaChannel().socket());//7
}

protected AbstractNioMessageChannel(Channel parent, SelectableChannel ch, int readInterestOp) {
    super(parent, ch, readInterestOp);
}

protected AbstractNioChannel(Channel parent, SelectableChannel ch, int readInterestOp) {
    super(parent);
    this.ch = ch;//8
    this.readInterestOp = readInterestOp;//9
    try {
        ch.configureBlocking(false);//10
    } catch (IOException e) {
        try {
            ch.close();
        } catch (IOException e2) {
            logger.warn(
                "Failed to close a partially initialized socket.", e2);
        }

        throw new ChannelException("Failed to enter non-blocking mode.", e);
    }
}

protected AbstractChannel(Channel parent) {
    this.parent = parent;//11
    id = newId();//12
    unsafe = newUnsafe();//13
    pipeline = newChannelPipeline();//14
}
```

> 1：实例化一个Channel实例，这里的Channel类型就是ServerBootstrap启动时配置的Channel类型，对于ServerBootstrap来说应为NioServerSocketChannel
>
> 2：初始化channel一些信息
>
> 3：通过java底层的API`SelectorProvider.provider()`获取一个SelectorProvider实例
>
> 5：利用JDK NIO底层api SelectorProvider.openServerSocketChannel()获取ServerSocketChannel实例
>
> 8：NioServerSocketChannel对象的成员变量为java底层ServerSocketChannel对象
>
> 9：NioServerSocketChannel对象只对SelectionKey.OP_ACCEPT事件感兴趣
>
> 10：配置ServerSocketChannel为非阻塞
>
> 11：设置当前Channel的父channel，这里父channel默认为空
>
> 12：设置一个channel的id
>
> 13：设置一个Unsafe对象，用来读写来自客户端的数据
>
> 14：设置NioServerSocketChannel接收请求的处理链，这里默认为DefaultChannelPipeline
>
> 15：注册当前ServerSocketChannel到Selector选择器中，并且绑定boss线程来进行事件轮询

初始化channel信息`ServerBootstrap#init(Channel channel)`

```java
void init(Channel channel) {
	//1
    setChannelOptions(channel, newOptionsArray(), logger);
    setAttributes(channel, attrs0().entrySet().toArray(EMPTY_ATTRIBUTE_ARRAY));

    ChannelPipeline p = channel.pipeline();

    final EventLoopGroup currentChildGroup = childGroup;
    final ChannelHandler currentChildHandler = childHandler;
    final Entry<ChannelOption<?>, Object>[] currentChildOptions;
    synchronized (childOptions) {
        currentChildOptions = childOptions.entrySet().toArray(EMPTY_OPTION_ARRAY);
    }
    final Entry<AttributeKey<?>, Object>[] currentChildAttrs = childAttrs.entrySet().toArray(EMPTY_ATTRIBUTE_ARRAY);
	//2
    p.addLast(new ChannelInitializer<Channel>() {
        @Override
        public void initChannel(final Channel ch) {
            final ChannelPipeline pipeline = ch.pipeline();
            ChannelHandler handler = config.handler();
            if (handler != null) {
                pipeline.addLast(handler);
            }
            ch.eventLoop().execute(new Runnable() {
                @Override
                public void run() {
                    pipeline.addLast(new ServerBootstrapAcceptor(
                        ch, currentChildGroup, currentChildHandler, currentChildOptions, currentChildAttrs)); //3
                }
            });
        }
    });
}
```

> 1：还记得在ServerBootstrap启动示例吗，我们会给ServerBootstrap配置一些启动参数，比如handler()，childHandler()，那些参数就是在这里使用的
>
> 2：NioServerSocketChannel中的ChannelPipeline添加一个特殊的处理器ChannelInitializer，此时ChannelPipeline链路结构如图所示
>
> ![image-20210114193017816](C:\Users\cdshenlong1\AppData\Roaming\Typora\typora-user-images\image-20210114193017816.png)
>
> 3：在接下来的部分会讲，当ServerSockerChannel注册到Selector中完成之后，会触发ChannelPipeline的handlerAdded事件，接着就会调用到2步骤中ChannelInitializer的initChannel方法，并且向ChannelPipeline中添加两个处理器，第一是如果ServerBootstrap配置的handler不为空，则添加。另一个则会添加一个ServerBootstrapAcceptor处理器，当server接收到新的client连接时，则会交给ServerBootstrapAcceptor处理。其实这里就是Doug Lea大师的Nio PPT分享中的Acceptor模块，ServerBootstrapAcceptor中具体细节在之后的《服务端怎么处理客户端连接》章节中分析。此时ChannelPipeline中的结构如下图所示。
>
> ![image-20210114195403446](C:\Users\cdshenlong1\AppData\Roaming\Typora\typora-user-images\image-20210114195403446.png)
>
> 当initChannel方法调用完了过后，会把ChannelInitializer从ChannelPipeline中移除掉，此时ChannelPipeline中的结构又变成了如下图所示。
>
> ![image-20210114195739603](C:\Users\cdshenlong1\AppData\Roaming\Typora\typora-user-images\image-20210114195739603.png)

接下来具体看一下Channel是怎么注册的，我们直接定位到`AbstractChannel#register0(ChannelPromise promise)`方法中，至于是怎么调用到这一步的，可以从上面的`initAndRegister()`方法的第15个步骤出发

```java
private void register0(ChannelPromise promise) {
    try {
        if (!promise.setUncancellable() || !ensureOpen(promise)) {
            return;
        }
        boolean firstRegistration = neverRegistered;
        doRegister(); //1
        neverRegistered = false;
        registered = true;
        pipeline.invokeHandlerAddedIfNeeded(); //2

        safeSetSuccess(promise);
        pipeline.fireChannelRegistered(); //3
        if (isActive()) { //4
            if (firstRegistration) {
                pipeline.fireChannelActive();//5
            } else if (config().isAutoRead()) {
                beginRead();//6
            }
        }
    } catch (Throwable t) {
        closeForcibly();
        closeFuture.setClosed();
        safeSetFailure(promise, t);
    }
}
```

> 1：最终会调用JDK底层的`ServerSocketChannel#register()`注册方法，将channel注册到Selector选择器中,在NioEventLoop进行事件轮询时，就可以监听到ServerSocketChannel感兴趣的事件。
>
> 2：在ChannelPipeline中传递HandlerAdded事件，会调用到`ChannelInitializer#initChannel`方法进行handler的添加操作
>
> 3：在ChannelPipeline中传递Registered事件，首先会传递到HeadContext中，但是HeadContext中基本没做什么特殊处理，然后传递到下一个Handler
>
> 4：判断当前是否处于激活状态
>
> 5：如果是第一次注册，并且处于激活状态，则传递Active事件
>
> 6：如果ServerBootstrap配置的自动读取，则直接开始读取

最后下面是一个ServerBootstrap启动的时序图

```sequence
Title:ServerBootStrap 启动流程
Note left of ServerBootStrap: 服务端启动引导类
ServerBootStrap -> ServerBootStrap:bind()
ServerBootStrap -> ServerBootStrap:doBind()
ServerBootStrap -> ServerBootStrap:initAndRegister()
ServerBootStrap -> NioServerSocketChannel:constructor()
ServerBootStrap -> ChannelPipeline:addLast(ChannelInitializer) -> addLast(ServerBootstrapAcceptor)
ServerBootStrap -> SingleThreadEventLoop:register()
SingleThreadEventLoop -> AbstractChannel:register()
AbstractChannel -> AbstractChannel:register0()
AbstractChannel -> AbstractNioChannel:doRegister()
AbstractChannel -> DefaultChannelPieline:invokeHandlerAddedIfNeeded()
DefaultChannelPieline -> DefaultChannelPieline:callHandlerAddedForAllHandlers()
```

```sequence
DefaultChannelPieline -> PendingHandlerAddedTask:execute()
PendingHandlerAddedTask --> DefaultChannelPieline:callHandlerAdded0()
DefaultChannelPieline -> AbstractChannelHandlerContext:callHandlerAdded()
AbstractChannelHandlerContext -> ChannelInitializer:handlerAdded()
ChannelInitializer -> ChannelInitializer:initChannel()
```

## NioEventLoop事件轮询

NioEventLoop是Netty中用来接收客户端获取服务端请求的唯一入口，我们先来看一下`NioEventLoop`的构造函数

```java
NioEventLoop(NioEventLoopGroup parent, Executor executor, SelectorProvider selectorProvider,
                 SelectStrategy strategy, RejectedExecutionHandler rejectedExecutionHandler,
                 EventLoopTaskQueueFactory queueFactory) {
    super(parent, executor, false, newTaskQueue(queueFactory), newTaskQueue(queueFactory),
          rejectedExecutionHandler);
    // 获取Selector对象的SelectorProvider对象
    this.provider = ObjectUtil.checkNotNull(selectorProvider, "selectorProvider");
    // 作为Select选择策略以及一次select的个数
    this.selectStrategy = ObjectUtil.checkNotNull(strategy, "selectStrategy");
    // Selector的包装
    final SelectorTuple selectorTuple = openSelector();
    // Selector选择器，用来轮询IO事件
    this.selector = selectorTuple.selector;
    // 没有包装的原始Selector选择器
    this.unwrappedSelector = selectorTuple.unwrappedSelector;
}

protected SingleThreadEventLoop(EventLoopGroup parent, Executor executor,
                                    boolean addTaskWakesUp, Queue<Runnable> taskQueue, Queue<Runnable> tailTaskQueue,
                                    RejectedExecutionHandler rejectedExecutionHandler) {
    super(parent, executor, addTaskWakesUp, taskQueue, rejectedExecutionHandler);
    // 尾部任务队列
    tailTasks = ObjectUtil.checkNotNull(tailTaskQueue, "tailTaskQueue");
}

protected SingleThreadEventExecutor(EventExecutorGroup parent, Executor executor,
                                        boolean addTaskWakesUp, Queue<Runnable> taskQueue,
                                        RejectedExecutionHandler rejectedHandler) {
    super(parent);
    // 新增任务时是否唤醒线程，默认为false
    this.addTaskWakesUp = addTaskWakesUp;
    //  最大任务等待数
    this.maxPendingTasks = DEFAULT_MAX_PENDING_EXECUTOR_TASKS;
    // 当前执行任务的线程池
    this.executor = ThreadExecutorMap.apply(executor, this);
    // 任务队列
    this.taskQueue = ObjectUtil.checkNotNull(taskQueue, "taskQueue");
    // 任务队列满时的异常处理器
    this.rejectedExecutionHandler = ObjectUtil.checkNotNull(rejectedHandler, "rejectedHandler");
}
```

> Netty是怎么实现一个Channel绑定一个线程，一个线程绑定多个Channel的呢？
>
> 从上面的代码段可以看出，一个AbstractChannel会持有一个NioEventLoop对象，而一个NioEventLoop持有一个Selector对象。当使用`SelectableChannel#register()`API将Channel注册到Selector选择器中时，由于使用的是NioEventLoop中的Selector对象，所以当NioEventLoop进行轮询时，就可以只轮询当前线程绑定的所有Channel对象。

接下来详细分析一下`NioEventLoop#run()`里的流程

```java
protected void run() {
    int selectCnt = 0;
    for (;;) {
        try {
            int strategy;
            try {
                // 获取selector策略
                strategy = selectStrategy.calculateStrategy(selectNowSupplier, hasTasks());
                switch (strategy) {
                    case SelectStrategy.CONTINUE:
                        continue;
                    case SelectStrategy.BUSY_WAIT:
                    case SelectStrategy.SELECT:
                        // 获取最近的定时任务开始时间，如果不存在定时任务，则为-1
                        long curDeadlineNanos = nextScheduledTaskDeadlineNanos();
                        if (curDeadlineNanos == -1L) {
                            curDeadlineNanos = NONE; // nothing on the calendar
                        }
                        nextWakeupNanos.set(curDeadlineNanos);
                        try {
                            if (!hasTasks()) {
                                // 如果不存在异步任务，直接执行I/O事件轮询
                                strategy = select(curDeadlineNanos);
                            }
                        } finally {
                            nextWakeupNanos.lazySet(AWAKE);
                        }
                    default:
                }
            } catch (IOException e) {
                // 当发生未知异常时，需重新生成一个新的Selector，替换老的Selector，并把注册到老的Selector上面的channel注册到新的Selector上去
                rebuildSelector0();
                selectCnt = 0;
                handleLoopException(e);
                continue;
            }
            // 省略部分代码
            if (ioRatio == 100) {
                try {
                    if (strategy > 0) {
                        // 处理轮询到的I/O事件
                        processSelectedKeys();
                    }
                } finally {
                    // 执行异步任务
                    ranTasks = runAllTasks();
                }
            } 
            // 省略部分代码
        } catch (CancelledKeyException e) {
            // 省略部分代码
        } catch (Throwable t) {
            handleLoopException(t);
        }
        // 省略部分代码
    }
}
```

> 获取SELECT策略时，需要用到一个`hasTasks()`方法，用来判断是否存在异步任务。如果存在异步任务，则会执行`selector.selectNow()`方法，该方法不会阻塞，接下来的switch就会跳到default分支。netty通过这种方式可以保证当存在异步任务时，优先执行异步任务。当不存在异步任务时，就会执行到`selector.select()`方法，在执行该方法之前，会通过`nextScheduledTaskDeadlineNanos()`方法获取最近的定时任务的开始时间curDeadlineNanos，如果不存在定时任务，则为Long类型的最大值。接下来会使用curDeadlineNanos作为select的超时时间，如果不存在定时任务，也就是curDeadlineNanos为Long的最大值，就会执行`selector.select()`方法，也就是不设置超时时间，直到有I/O事件为止才会返回。如果存在定时任务，则会通过定时任务的开始时间计算出select操作的超时时间。
>
> 当有I/O事件响应时，则会通过`processSelectedKeys()`方法处理I/O事件，下面是代码段

```java
private void processSelectedKeys() {
    if (selectedKeys != null) {
        processSelectedKeysOptimized();
    } else {
        processSelectedKeysPlain(selector.selectedKeys());
    }
}
```

> 从代码可以看出，这里处理有两个分支，一个是处理优化过的SelectedKey，另一个分支是处理正常的SelectedKey，什么是优化过的key？这里就需要看一下selectedKeys变量的数据结构。跟踪代码可以看出，selectedKeys的数据类型是`SelectedSelectionKeySet`，`SelectedSelectionKeySet`底层存储结构是用的数组，所以它的遍历效率比较高,原生的存储结构是HashSet，其实也就是HashMap，它的遍历效率没有数组高。当通过`SelectorProvider#openSelector()`方法获取一个selector时，这里有个特殊操作，就是一个优化开关，如果开关关闭，则使用原生的selectedKeys存储结构，也就是HashSet。如果开关打开，就会使用反射，将SelectorImpl类中的publicKeys与publicSelectedKeys字段替换成`SelectedSelectionKeySet`的存储类型。可以看下面的代码片段

```java
private SelectorTuple openSelector() {
    final Selector unwrappedSelector;
    try {
        // 获取一个Selector实例
        unwrappedSelector = provider.openSelector();
    } catch (IOException e) {
        throw new ChannelException("failed to open a new selector", e);
    }

    if (DISABLE_KEY_SET_OPTIMIZATION) {
        return new SelectorTuple(unwrappedSelector);
    }
	// 获取SelectorImpl的class
    Object maybeSelectorImplClass = AccessController.doPrivileged(new PrivilegedAction<Object>() {
        @Override
        public Object run() {
            try {
                return Class.forName(
                    "sun.nio.ch.SelectorImpl",
                    false,
                    PlatformDependent.getSystemClassLoader());
            } catch (Throwable cause) {
                return cause;
            }
        }
    });
	// 省略部分代码
    final Class<?> selectorImplClass = (Class<?>) maybeSelectorImplClass;
    final SelectedSelectionKeySet selectedKeySet = new SelectedSelectionKeySet();
    Object maybeException = AccessController.doPrivileged(new PrivilegedAction<Object>() {
        @Override
        public Object run() {
            try {
                // 反射获取selectedKey字段
                Field selectedKeysField = selectorImplClass.getDeclaredField("selectedKeys");
                Field publicSelectedKeysField = selectorImplClass.getDeclaredField("publicSelectedKeys");
				// 省略部分代码
                Throwable cause = ReflectionUtil.trySetAccessible(selectedKeysField, true);
                if (cause != null) {
                    return cause;
                }
                cause = ReflectionUtil.trySetAccessible(publicSelectedKeysField, true);
                if (cause != null) {
                    return cause;
                }
                // 反射方式设置selectedKeys为SelectedSelectionKeySet类型
                selectedKeysField.set(unwrappedSelector, selectedKeySet);
                // 反射方式设置publicSelectedKeys为SelectedSelectionKeySet类型
                publicSelectedKeysField.set(unwrappedSelector, selectedKeySet);
                return null;
            } catch (NoSuchFieldException e) {
                return e;
            } catch (IllegalAccessException e) {
                return e;
            }
        }
    });
	// 省略部分代码
}
```

接下来再看一下`processSelectedKeysOptimized()`方法的具体逻辑

```java
private void processSelectedKeysOptimized() {
    for (int i = 0; i < selectedKeys.size; ++i) {
        final SelectionKey k = selectedKeys.keys[i];
        selectedKeys.keys[i] = null;
        // 获取I/O事件类型
        final Object a = k.attachment();
        if (a instanceof AbstractNioChannel) {
            // 处理I/O事件
            processSelectedKey(k, (AbstractNioChannel) a);
        } else {
            @SuppressWarnings("unchecked")
            NioTask<SelectableChannel> task = (NioTask<SelectableChannel>) a;
            // 处理异步任务
            processSelectedKey(k, task);
        }
		// 是否需要重新轮询
        if (needsToSelectAgain) {
            // 将i+1之前的I/O事件都置为空
            selectedKeys.reset(i + 1);
            //重新轮询
            selectAgain();
            // 将i置为-1，接下来会执行一次i++，然后i又会从0开始执行I/O事件
            i = -1;
        }
    }
}
```

> 这里开始处理I/O事件，大家还记得将channel注册到selector时，可以填写一个attach参数。在这就可以通过`attachment()`方法获取事件类型，如果是`AbstractNioChannel`类型，则处理I/O事件，否则处理异步任务。接下来有一个needsToSelectAgain标识，表示是否需要重新轮询一次I/O事件，为什么会有这个标识呢？我们先跟踪一下这个标识是在哪里写入的，最终发现，当channel从Selector取消注册超过256次时，就会将这个标志位置为true。

接下来继续看处理I/O事件的详细逻辑`processSelectedKey()`

```java
private void processSelectedKey(SelectionKey k, AbstractNioChannel ch) {
        final AbstractNioChannel.NioUnsafe unsafe = ch.unsafe();
        // 省略部分代码
        try {
            int readyOps = k.readyOps();
            // 处理连接事件
            if ((readyOps & SelectionKey.OP_CONNECT) != 0) {
                int ops = k.interestOps();
                ops &= ~SelectionKey.OP_CONNECT;
                k.interestOps(ops);
                // 在ChinnelPipeline上传递Connect事件
                unsafe.finishConnect();
            }

            // 处理可写事件
            if ((readyOps & SelectionKey.OP_WRITE) != 0) {
                ch.unsafe().forceFlush();
            }

            // 处理客户端的读事件与接收事件
            if ((readyOps & (SelectionKey.OP_READ | SelectionKey.OP_ACCEPT)) != 0 || readyOps == 0) {
                // 在ChinnelPipeline上传递Read事件
                unsafe.read();
            }
        } catch (CancelledKeyException ignored) {
            unsafe.close(unsafe.voidPromise());
        }
    }
```

> Connect事件的传递比较简单，感兴趣的同学可以自己去看一下源码，这里我们主要看一下读写事件。
>
> 首先看一下写事件，从下面的代码可以看出，主要是把buffer里的数据刷新到socket缓冲区，然后发送到客户端

```java
protected void flush0() {
    if (inFlush0) {
        // Avoid re-entrance
        return;
    }
    final ChannelOutboundBuffer outboundBuffer = this.outboundBuffer;
    if (outboundBuffer == null || outboundBuffer.isEmpty()) {
        return;
    }
    inFlush0 = true;
	// 省略部分代码
    try {
        // 真正的将数据写到客户端，底层会调用Nio的SocketChannel#write()方法
        doWrite(outboundBuffer);
    } catch (Throwable t) {
       // 省略部分代码
    } finally {
        inFlush0 = false;
    }
}
```

下面我们再来看一下Netty是怎么处理读事件的

```java
public void read() {
    assert eventLoop().inEventLoop();
    final ChannelConfig config = config();
    final ChannelPipeline pipeline = pipeline();
    // 获取一个netty自己的内存分配器
    final RecvByteBufAllocator.Handle allocHandle = unsafe().recvBufAllocHandle();
    allocHandle.reset(config);
    boolean closed = false;
    Throwable exception = null;
    try {
        try {
            do {
                // 将数据读取到readBuf中
                int localRead = doReadMessages(readBuf);
                if (localRead == 0) {
                    break;
                }
                if (localRead < 0) {
                    closed = true;
                    break;
                }
				// 增加读的次数
                allocHandle.incMessagesRead(localRead);
            } while (allocHandle.continueReading());
        } catch (Throwable t) {
            exception = t;
        }

        int size = readBuf.size();
        for (int i = 0; i < size; i ++) {
            readPending = false;
            // 在ChannelPipeline上传播read事件
            pipeline.fireChannelRead(readBuf.get(i));
        }
        // 清空读的缓冲区
        readBuf.clear();
        // 读取完成，记录读取总的字节数
        allocHandle.readComplete();
        // 传播一次读取完成的事件，一次读取完成可能包含多个SocketChannel
        pipeline.fireChannelReadComplete();

        if (exception != null) {
            closed = closeOnReadError(exception);

            pipeline.fireExceptionCaught(exception);
        }

        if (closed) {
            inputShutdown = true;
            if (isOpen()) {
                close(voidPromise());
            }
        }
    } finally {
        if (!readPending && !config.isAutoRead()) {
            // 移除read 事件
            removeReadOp();
        }
    }
}
```

> 当Netty处理读事件时，首先会获取一个内存分配处理器，读取消息到分配的内存里（Netty内存分配器后面分析），然后在ChannelPipeline上传播读事件，最终会在channel上移除read事件。

最后我们来看一下Netty是怎么处理异步任务的

```java
protected boolean runAllTasks(long timeoutNanos) {
    // 从定时任务队列中取出任务添加到普通任务队列中
    fetchFromScheduledTaskQueue();
    // 从任务列表中取出一个任务
    Runnable task = pollTask();
    if (task == null) {
        // 如果任务为空，表示没有普通任务可执行，直接执行tailTask任务队列中的任务
        afterRunningAllTasks();
        return false;
    }
	// 计算执行任务的截止时间（相对时间，当前时间减去服务启动时间再加上超时时间）
    final long deadline = timeoutNanos > 0 ? ScheduledFutureTask.nanoTime() + timeoutNanos : 0;
    long runTasks = 0;
    long lastExecutionTime;
    for (;;) {
        // 安全的执行任务
        safeExecute(task);
        runTasks ++;
        // 执行任务个数和0x3f（0011 1111）进行与运算，这里指的是当任务个数为64个时，进行一次截止时间判断
        if ((runTasks & 0x3F) == 0) {
            lastExecutionTime = ScheduledFutureTask.nanoTime();
            // 如果当前时间大于了截止时间，则中断执行任务，这是为了把时间留出一部分来执行I/O事件
            if (lastExecutionTime >= deadline) {
                break;
            }
        }
        task = pollTask();
        // 如果任务为空，终止循环
        if (task == null) {
            lastExecutionTime = ScheduledFutureTask.nanoTime();
            break;
        }
    }
    // 最后执行尾部任务队列中的任务
    afterRunningAllTasks();
    this.lastExecutionTime = lastExecutionTime;
    return true;
}
```

> 首选执行异步任务有一个超时，通过这个超时时间来计算任务可以执行多久。首先netty会把定时任务合并到普通任务队列中，然后判断这个任务队列中是否有任务，如果没有任务，则先执行尾部任务队列，然后提前返回。如果有任务，则会计算任务的执行截止时间，每当任务执行了64个时，都会判断一下当前时间是否大于截止时间，如果大于了截止时间，则停止执行普通任务，然后再执行尾部任务队列。

## ChannelPipeline事件传播与异常处理

ChannelPipeline是Netty中非常非常非常重要的一个组件，Netty的事件传播以及我们自定义的业务处理，都是基于ChannelPipeline来实现的。在分析ChannelPipeline之前，我们先来了解一下与ChannelPipeline相关的另外三个超级重要的组件`ChannelHandler`、`ChannelInboundHandler`、`ChannelOutboundHandler`、`ChannelInboundInvoker`、`ChannelOutboundInvoker`，接下来我们就详细分析一下这几个组件的作用

> `ChannelHandler`是`ChannelInboundHandler`和`ChannelOutboundHandler`的父类，里面定义了以下三个最基础的方法以及一个注解

```java
public interface ChannelHandler {
	// 新建客户端连接触发
    void handlerAdded(ChannelHandlerContext ctx) throws Exception;

	// 客户端中断连接触发
    void handlerRemoved(ChannelHandlerContext ctx) throws Exception;

    // 当发生异常时触发
    @Deprecated
    void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception;

    // 标识ChannelHandler是否可同时添加到不同的ChannelPipeline
    @Inherited
    @Documented
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Sharable {
        // no value
    }
}
```

> `ChannelInboundHandler`定义了一系列客户端连接消息事件处理。我们可以这样理解，当有客户端连接或者当客户端发消息到服务端时，消息的流向是从客户端到服务端，对于服务端来说，消息就是流进来。所以当消息流进来时，会经过一系列的`ChannelInboundHandler`处理，`ChannelInboundHandler`中定义了很多方法，如下所示，比如：客户端连接事件，注册事件，激活事件，消息读取事件等等

```java
public interface ChannelInboundHandler extends ChannelHandler {
	// 客户端注册事件
    void channelRegistered(ChannelHandlerContext ctx) throws Exception;
	// 客户端取消注册事件
    void channelUnregistered(ChannelHandlerContext ctx) throws Exception;
	// 激活事件
    void channelActive(ChannelHandlerContext ctx) throws Exception;
	// 取消激活事件
    void channelInactive(ChannelHandlerContext ctx) throws Exception;
	// 消息读取事件
    void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception;
	// 消息读取完成事件
    void channelReadComplete(ChannelHandlerContext ctx) throws Exception;
	// 用户事件触发事件
    void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception;
	// Channel通道是否可读状态变更事件
    void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception;
	// 异常处理事件
    @Override
    @SuppressWarnings("deprecation")
    void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception;
```

> `ChannelOutboundHandler`定义了一系列消息流出事件，对于服务端来说，当需要把消息回写给客户端时，就会经过`ChannelOutboundHandler`上的一系列事件处理。比如当发消息时，需要将消息进行编码处理，这时就是通过扩展`ChannelOutboundHandler`来实现

```java
public interface ChannelOutboundHandler extends ChannelHandler {
	// 调用一次绑定操作
    void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise) throws Exception;

   // 调用一次连接操作
    void connect(
            ChannelHandlerContext ctx, SocketAddress remoteAddress,
            SocketAddress localAddress, ChannelPromise promise) throws Exception;

    // 调用一次中断连接操作
    void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception;

    //调用一次关闭连接操作
    void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception;

    // 调用一次取消注册操作，比如在NioEvevtLoop事件轮询时，取消Channel的注册就可触发该事件
    void deregister(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception;

    // 拦截 {@link ChannelHandlerContext#read()}的读事件
    void read(ChannelHandlerContext ctx) throws Exception;

    // 写数据事件
    void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception;

    // 刷数据到客户端事件
    void flush(ChannelHandlerContext ctx) throws Exception;
}
```

> `ChannelInboundInvoker`的作用就是中间传递Inbound事件，然后疯狂调用`ChannelInboundHandler`类中的方法，`ChannelOutboundInvoker`传递Outbound事件，调用`ChannelOutboundHandler`类中的方法

接下来就到了我们的重头戏了，`ChannelPipeline`有一个默认的实现类`DefaultChannelPipeline`，每个SocketChannel都会绑定一个`DefaultChannelPipeline`，当接收到SocketChannel事件时，Netty就会把事件传递给`DefaultChannelPipeline`。我们着重分析一下这个类，先看下`DefaultChannelPipeline`的构造函数

```java
protected DefaultChannelPipeline(Channel channel) {
    // 当前绑定的SocketChannel
    this.channel = ObjectUtil.checkNotNull(channel, "channel");
    // 一个channel的回调管理
    succeededFuture = new SucceededChannelFuture(channel, null);
    // 这也是一个channel的回调管理
    voidPromise =  new VoidChannelPromise(channel, true);
	// pipeline的尾节点
    tail = new TailContext(this);
    // pipeline的头节点
    head = new HeadContext(this);
    // 设置头节点的下一个节点是尾节点
    head.next = tail;
    // 设置尾节点的下一个节点是头节点
    tail.prev = head;
}
```

> `DefaultChannelPipeline`的内部结构是一个双向链表，当初始化`DefaultChannelPipeline`时，会初始化`DefaultChannelPipeline`相关联的`SocketChannel`，并且在链表上会初始化两个节点，一个头节点`HeadContext`，一个尾节点`TailContext`。链表上的元素其实都是`ChannelHandlerContext`，它会包装一个`ChannelHandler`，并且会保存一些上下文信息，比如当前`ChannelHandlerContext`关联的`DefaultChannelPipeline`对象等。当数据流入时，会从`HeadContext`传递到`TailContext`，数据流出时，会从`TailContext`传递到`HeadContext`，所以`HeadContext`有两个非常重要的职责，一是读取来自客户端的数据，二是往客户端写入数据。接下来我们详细分析一下`HeadContext`读数据与写数据职责。

```java
final class HeadContext extends AbstractChannelHandlerContext
            implements ChannelOutboundHandler, ChannelInboundHandler {

    private final Unsafe unsafe;

    HeadContext(DefaultChannelPipeline pipeline) {
        super(pipeline, null, HEAD_NAME, HeadContext.class);
        unsafe = pipeline.channel().unsafe();
        setAddComplete();
    }
    // 省略部分代码
    @Override
    public void read(ChannelHandlerContext ctx) {
        // 开始读取来自客户端的数据
        unsafe.beginRead();
    }
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        // 开始往缓冲区写数据
        unsafe.write(msg, promise);
    }
    @Override
    public void flush(ChannelHandlerContext ctx) {
        // 刷缓冲区数据到客户端
        unsafe.flush();
    }
    // 省略部分代码
}
```

读客户端数据比较简单，只是调用了一个`unsafe.beginRead()`方法，而该方法的具体实现可以看下面代码片段，只是修改了一个是否正在读取标识以及移除了读事件

```java
protected void doBeginRead() throws Exception {
    // Channel.read() or ChannelHandlerContext.read() was called
    final SelectionKey selectionKey = this.selectionKey;
    if (!selectionKey.isValid()) {
        return;
    }
	// 等待读取中的这个表示置为true，表示正在读取
    readPending = true;
    final int interestOps = selectionKey.interestOps();
    if ((interestOps & readInterestOp) == 0) {
        // 移除读事件
        selectionKey.interestOps(interestOps | readInterestOp);
    }
}
```

我们再来看看下面的写数据流程，首先是获取当前的`ChannelOutboundBuffer`，如果为空，则提前返回。接着就是过滤消息以及计算消息的大小，为之后的添加数据到缓冲区作准备。

```java
public final void write(Object msg, ChannelPromise promise) {
    assertEventLoop();
	// 获取数据缓冲区
    ChannelOutboundBuffer outboundBuffer = this.outboundBuffer;
    if (outboundBuffer == null) {
        // 如果数据缓冲区为空，则触发失败回调并提前返回
        safeSetFailure(promise, newClosedChannelException(initialCloseCause));
        ReferenceCountUtil.release(msg);
        return;
    }
    int size;
    try {
        // 过滤消息
        msg = filterOutboundMessage(msg);
        // 获取消息大小
        size = pipeline.estimatorHandle().size(msg);
        if (size < 0) {
            size = 0;
        }
    } catch (Throwable t) {
        safeSetFailure(promise, t);
        ReferenceCountUtil.release(msg);
        return;
    }
	// 将数据写到缓冲区
    outboundBuffer.addMessage(msg, size, promise);
}
```

详细分析一下`outboundBuffer.addMessage(msg, size, promise)`方法，看看Netty到底是怎么把数据追加到缓冲区的

```java
public void addMessage(Object msg, int size, ChannelPromise promise) {
    // 把消息封装成Entry对象
    Entry entry = Entry.newInstance(msg, size, total(msg), promise);
    if (tailEntry == null) {
        flushedEntry = null;
    } else {
        // 如果当前队列不为空，则将尾节点的下一个节点设置为新添加的节点
        Entry tail = tailEntry;
        tail.next = entry;
    }
    // 将尾节点设置为当前节点
    tailEntry = entry;
    if (unflushedEntry == null) {
        unflushedEntry = entry;
    }
	// 增加缓冲区已用大小
    incrementPendingOutboundBytes(entry.pendingSize, false);
}
private void incrementPendingOutboundBytes(long size, boolean invokeLater) {
    if (size == 0) {
        return;
    }
	// 追加后的缓冲区已用大小
    long newWriteBufferSize = TOTAL_PENDING_SIZE_UPDATER.addAndGet(this, size);
    if (newWriteBufferSize > channel.config().getWriteBufferHighWaterMark()) {
        // 如果已用大小大于配置的最高可写水位，则设置当前已不可写，并且发送Channel可写状态变更事件
        setUnwritable(invokeLater);
    }
}
private void setUnwritable(boolean invokeLater) {
    for (;;) {
        final int oldValue = unwritable;
        final int newValue = oldValue | 1;
        // 使用CAS更新可写状态
        if (UNWRITABLE_UPDATER.compareAndSet(this, oldValue, newValue)) {
            if (oldValue == 0 && newValue != 0) {
                // 如果状态更新成功，并且从可写变为不可写，则传递可写状态变更事件
                fireChannelWritabilityChanged(invokeLater);
            }
            break;
        }
    }
}
```

`ChannelOutboundBuffer`内部结构也是一个单向链表，里面有几个比较重要的属性，flushedEntry表示链表上第一个刷新到客户端的数据，unflushedEntry表示链表上第一个没有刷新到客户端的数据，tailEntry表示链表的尾节点。我们通过下面一个图来表示数据的追加过程

![addMessage](D:\document\个人\学习日记\Netty画图\addMessage.png)

我们再来看看数据的刷新到客户端的过程

```java
public final void flush() {
    assertEventLoop();

    ChannelOutboundBuffer outboundBuffer = this.outboundBuffer;
    if (outboundBuffer == null) {
        return;
    }
	// 修改一些刷新数据标识
    outboundBuffer.addFlush();
    // 正儿八经执行刷新数据到客户端逻辑
    flush0();
}
public void addFlush() {
    // 获取链表上第一个未被刷新的数据
    Entry entry = unflushedEntry;
    if (entry != null) {
        if (flushedEntry == null) {
            // 如果刷新的第一个数据为空，则把第一个刷新的数据置为第一个未被刷新的数据
            flushedEntry = entry;
        }
        do {
            flushed ++;
            if (!entry.promise.setUncancellable()) {
                // 调用取消方法保证释放内存
                int pending = entry.cancel();
                // 减少buffer的使用量
                decrementPendingOutboundBytes(pending, false, true);
            }
            entry = entry.next;
        } while (entry != null);

        // 当数据刷新完了过后，将未被刷新的标识置为null
        unflushedEntry = null;
    }
}
private void decrementPendingOutboundBytes(long size, boolean invokeLater, boolean notifyWritability) {
    if (size == 0) {
        return;
    }
	// 减少buffer的使用量
    long newWriteBufferSize = TOTAL_PENDING_SIZE_UPDATER.addAndGet(this, -size);
    if (notifyWritability && newWriteBufferSize < channel.config().getWriteBufferLowWaterMark()) {
        // 如果buffer的使用量小于Channel配置的buffer最低水位，则表示buffer可写
        setWritable(invokeLater);
    }
}
private void setWritable(boolean invokeLater) {
    for (;;) {
        final int oldValue = unwritable;
        final int newValue = oldValue & ~1;
        // 使用CAS更新可写状态为可写
        if (UNWRITABLE_UPDATER.compareAndSet(this, oldValue, newValue)) {
            if (oldValue != 0 && newValue == 0) {
                // 传递可写状态为可写的事件
                fireChannelWritabilityChanged(invokeLater);
            }
            break;
        }
    }
}
```

我们也从下面一张图来表示数据刷新过后buffer标识的最终形态，如果觉得有点不明白的话，可以结合上面那张数据写入的图与源码一起再分析一下，相信多看两遍就可以看懂了

![addFlush](D:\document\个人\学习日记\Netty画图\addFlush.png)

最后我们看看`flush0()`方法

```java
protected void flush0() {
    if (inFlush0) {
        // Avoid re-entrance
        return;
    }

    final ChannelOutboundBuffer outboundBuffer = this.outboundBuffer;
    if (outboundBuffer == null || outboundBuffer.isEmpty()) {
        return;
    }

    inFlush0 = true;
	// 省略部分代码
    try {
        // 执行数据刷新
        doWrite(outboundBuffer);
    } catch (Throwable t) {
        // 省略部分代码
    } finally {
        inFlush0 = false;
    }
}

// NioSocketChannel#doWrite()
protected void doWrite(ChannelOutboundBuffer in) throws Exception {
    // 获取当前的客户端Channel
    SocketChannel ch = javaChannel();
    // 配置可写多少次
    int writeSpinCount = config().getWriteSpinCount();
    do {
        if (in.isEmpty()) {
            // 如果buffer里没有数据，清除写事件
            clearOpWrite();
            return;
        }
        int maxBytesPerGatheringWrite = ((NioSocketChannelConfig) config).getMaxBytesPerGatheringWrite();
        // 将Netty的buffer转换成java NIO的ByteBuffer
        ByteBuffer[] nioBuffers = in.nioBuffers(1024, maxBytesPerGatheringWrite);
        // 计算有几个buffer可写
        int nioBufferCnt = in.nioBufferCount();

        switch (nioBufferCnt) {
            case 0:
                // 当ByteBuffer为0时，我们可能还有其他东西要写，所以这里回退到普通的写操作
                writeSpinCount -= doWrite0(in);
                break;
            case 1: {
                // 有一个ByteBufer可写，所以这里获取第一个ByteBuffer
                ByteBuffer buffer = nioBuffers[0];
                // 需要写的数据大小
                int attemptedBytes = buffer.remaining();
                // 调用JAVA原生NIO的API执行写操作
                final int localWrittenBytes = ch.write(buffer);
                if (localWrittenBytes <= 0) {
                    incompleteWrite(true);
                    return;
                }
                adjustMaxBytesPerGatheringWrite(attemptedBytes, localWrittenBytes, maxBytesPerGatheringWrite);
                // 移除已写的数据
                in.removeBytes(localWrittenBytes);
                // 可写次数减一
                --writeSpinCount;
                break;
            }
            default: {
                long attemptedBytes = in.nioBufferSize();
                // 如果有多个ByteBuffer需要写，则调用NIO的批量写 操作
                final long localWrittenBytes = ch.write(nioBuffers, 0, nioBufferCnt);
                if (localWrittenBytes <= 0) {
                    incompleteWrite(true);
                    return;
                }
                adjustMaxBytesPerGatheringWrite((int) attemptedBytes, (int) localWrittenBytes,
                                                maxBytesPerGatheringWrite);
                // 移除已写的数据
                in.removeBytes(localWrittenBytes);
                // 可写次数减一
                --writeSpinCount;
                break;
            }
        }
    } while (writeSpinCount > 0);
	// 如果数据刷完了，则移除写事件，如果数据没有刷完，则会再执行一次刷新操作
    incompleteWrite(writeSpinCount < 0);
}
```

上面，我们把HeadContext的读写数据重要流程分析完了，接下来，我们看一下事件是怎么在`DefaultChannelPipeline`的链表上传播的，先看一下`AbstractChannelHandlerContext#fireChannelRead()`方法

```java
public ChannelHandlerContext fireChannelRead(final Object msg) {
    // 调用实现了ChannelInboundHandler的方法
    invokeChannelRead(findContextInbound(MASK_CHANNEL_READ), msg);
    return this;
}

// 查找ChannelInboundHandler
private AbstractChannelHandlerContext findContextInbound(int mask) {
    AbstractChannelHandlerContext ctx = this;
    EventExecutor currentExecutor = executor();
    do {
        // 当前Context的下一个Context，对于第一次进来，当前Context就是HeadContext
        ctx = ctx.next;
    } while (skipContext(ctx, currentExecutor, mask, MASK_ONLY_INBOUND));// 会忽略掉不是ChannelInboundHandler类和标注了@Skip注解的类
    return ctx;
}
private static boolean skipContext(
            AbstractChannelHandlerContext ctx, EventExecutor currentExecutor, int mask, int onlyMask) {
    return (ctx.executionMask & (onlyMask | mask)) == 0 ||
        (ctx.executor() == currentExecutor && (ctx.executionMask & mask) == 0);
}
static void invokeChannelRead(final AbstractChannelHandlerContext next, Object msg) {
    // 针对ReferenceCounted类型的消息做特殊处理
    final Object m = next.pipeline.touch(ObjectUtil.checkNotNull(msg, "msg"), next);
    EventExecutor executor = next.executor();
    if (executor.inEventLoop()) {
        // 实际调用Channel的Read方法进行数据的读取进行传播
        next.invokeChannelRead(m);
    } else {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                // 实际调用Channel的Read方法进行数据的读取进行传播
                next.invokeChannelRead(m);
            }
        });
    }
}
```

总结一下，对于数据流入来说，`DefaultChannelPipeline`的处理链路是HeadContext到TailContext，然后中间只找ChannelInboundHandler的实现类以及没有被@Skip注解标注的方法，通过ChannelHandlerContext的next元素，一个一个的执行read方法，最终就会调用到TailContext的read方法然后结束。此时你就可能会想，要是我在调用过程中出现了未知异常怎么办？下面我们接着分析一下Netty是怎么处理执行链路过程中产生的异常，`AbstractChannelHandlerContext`有个fireExceptionCaught方法，它就是用来传递异常的，我们先看看以下代码片段

```java
public ChannelHandlerContext fireExceptionCaught(final Throwable cause) {
    // 通过MASK_EXCEPTION_CAUGHT掩码查找ChannelInboundHandler实现了exceptionCaught的方法
    invokeExceptionCaught(findContextInbound(MASK_EXCEPTION_CAUGHT), cause);
    return this;
}

static void invokeExceptionCaught(final AbstractChannelHandlerContext next, final Throwable cause) {
    ObjectUtil.checkNotNull(cause, "cause");
    EventExecutor executor = next.executor();
    if (executor.inEventLoop()) {
        // 调用异常处理方法
        next.invokeExceptionCaught(cause);
    } else {
        try {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    next.invokeExceptionCaught(cause);
                }
            });
        } catch (Throwable t) {
        }
    }
}
```

> 其实异常的处理链路和读取操作的处理链路模式是基本一致的，只是一个调用的是read方法，一个调用的是exceptionCaught方法。我们可以看出Netty查找异常方法时，是用的`AbstractChannelHandlerContext`的next元素向后查找的，所以当我们使用ChannelHandler进行统一的异常处理时，应把异常处理的ChannelHandler添加到`DefaultChannelPipeline`处理链的最后，这样才能捕获所有的业务异常

接下来我们再看一下数据的流出，也就是数据的写入链路是怎样的，我们从方法`Channel#writeAndFlush()`入口开始看，Channel接口继承于`ChannelOutboundInvoker`接口，最终是通过`AbstractChannel`类来实现的，所以这里我们直接从`AbstractChannel`类看

```java
// AsbtractChannel
public ChannelFuture writeAndFlush(Object msg) {
    return pipeline.writeAndFlush(msg);
}
// DefaultChannelPipeline
 public final ChannelFuture writeAndFlush(Object msg) {
     return tail.writeAndFlush(msg);
 }
```

从上面的代码片段可以看出，`AbstractChannel#writeAndFlush()`方法其实是直接转调的`DefaultChannelPipeline#writeAndFlush()`方法，然后`DefaultChannelPipeline`又会去调`TailContext#writeAndFlush`方法，所以我们再来分析`TailContext#writeAndFlush()`方法

```java
public ChannelFuture writeAndFlush(Object msg) {
    return writeAndFlush(msg, newPromise());
}

public ChannelFuture writeAndFlush(Object msg, ChannelPromise promise) {
    write(msg, true, promise);
    return promise;
}

private void write(Object msg, boolean flush, ChannelPromise promise) {
    ObjectUtil.checkNotNull(msg, "msg");
	// 省略部分代码
    // 查找ChannelOutboundHandlerContext类型的write方法，并且会忽略掉标注了@Skip注解的方法
    final AbstractChannelHandlerContext next = findContextOutbound(flush ?
                                                                   (MASK_WRITE | MASK_FLUSH) : MASK_WRITE);
    final Object m = pipeline.touch(msg, next);
    EventExecutor executor = next.executor();
    if (executor.inEventLoop()) {
        if (flush) {
            // 写入并刷新缓冲区
            next.invokeWriteAndFlush(m, promise);
        } else {
            // 写入缓冲区
            next.invokeWrite(m, promise);
        }
    } else {
        // 如果不是当前事件轮询线程，则封装成一个异步任务
        final WriteTask task = WriteTask.newInstance(next, m, promise, flush);
        // 安全的执行任务，其实也就是捕获了异常
        if (!safeExecute(executor, task, promise, m, !flush)) {
            // 如果任务执行失败，则需要将缓冲区里面新加的数据清理掉，避免浪费缓冲区
            task.cancel();
        }
    }
}
```

从上面的代码片段可以看出，数据写入到客户端，是从TailContext流向到HeadContext，并且查找的是`ChannelOutboundHandlerContext`类型的类来做处理，最终通过`HeadContext#write()`方法回写到客户端，

写入处理链发生异常的处理方式和上面的读取异常处理方式是一样的，所以这里就不再分析了

### 总结

`DefaultChannelPipeline`是Netty数据处理链最重要的部分，它的核心就是数据读取是从HeadContext传递到TailContext，数据写入是从TailContext传递到HeadContext，所以`DefaultChannelPipeline`设计的是一个双向链表，对我们用户来说，我们自定义的业务处理Handler就添加在HeadContext与TailContext之间，数据的读取和数据的写入都是在HeadContext中进行。在处理链中，通过掩码的方式来筛选符合条件的ChannelHandler，这种设计真的非常的巧妙，我们平时工作中也可以参考一波

