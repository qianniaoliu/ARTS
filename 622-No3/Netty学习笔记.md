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

我们先来看一下`NioEventLoop`的构造函数

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

> 当Netty处理读事件时，首先会获取一个内存分配处理器，读取消息到分配的内存里，然后在ChannelPipeline上传播读事件，最终会在channel上移除read事件



