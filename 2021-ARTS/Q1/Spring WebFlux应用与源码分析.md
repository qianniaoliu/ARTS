# Spring WebFlux应用与源码分析

## 编程模型

### Functional Endpoints（函数式端点）

```java
/**
 * 初始化路由信息
 *
 * @return {@link RouterFunction}
 */
@Bean
public RouterFunction routerFunction() {
    return RouterFunctions.route()
        .GET("/router", RequestPredicates.accept(MediaType.APPLICATION_JSON), this::helloRouter)
        .build();
}

/**
 * http请求具体的执行方法
 *
 * @param serverRequest 请求参数
 * @return 返回对象
 */
private Mono<ServerResponse> helloRouter(ServerRequest serverRequest) {
    return ServerResponse.ok().body(Mono.just("Hello Router"), String.class);
}
```

这里首先定义了一个名叫helloRouter的方法，他的入参是`ServerRequest`，出参是`ServerResponse`，`ServerRequest`其实就类似于Servlet中的`HttpServletRequest`，`ServerResponse`类似于Servlet中的`HttpServletResponse`，都可以获取请求中的一些参数，比如：headers，attributes。另外定义了一个`RouterFunction`类型的Bean，利用RouterFunctions和RequestPredicates的API，定义HTTP请求的路径以及媒体类型，然后生成RouterFunction对象。当接收到客户端请求时，Spring就会利用RouterFunction对象来解析请求以及做请求路由，具体路由策略下面我们会分析。

### Annotated Controllers（注解驱动）

```java
@RestController
public class IndexController {
    @GetMapping("/test/requestParam")
    public String requestParam(@RequestParam String hello){
        return hello;
    }
}
```

注解驱动的使用方式其实和Spring MVC一模一样，定义最基本的`@Controller`和`@RequestMapping`就可以使用了，这种模式就不详细描述了。接下来进行Spring WebFlux的请求处理流程分析

## DispatcherHandler请求处理流程

### DispatcherHandler源码分析

`DispatcherHandler`是接收请求处理的第一步，所以我们这里先详细分析一下`DispatcherHandler`的源码，下面是`DispatcherHandler`一部分源码

```java
public class DispatcherHandler implements WebHandler, ApplicationContextAware {
    // 请求处理器的映射关系集合
	@Nullable
	private List<HandlerMapping> handlerMappings;
    // 执行具体的HandlerFunction适配器集合
	@Nullable
	private List<HandlerAdapter> handlerAdapters;
    // 返回结果的处理器集合
	@Nullable
	private List<HandlerResultHandler> resultHandlers;
    //默认构造器
	public DispatcherHandler() {
	}
	// 省略部分代码...
    // Bean类型是ApplicationContextAware的默认回调方法
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		initStrategies(applicationContext);
	}
	protected void initStrategies(ApplicationContext context) {
        // 通过IOC容器获取所有类型为HandlerMapping的Bean
		Map<String, HandlerMapping> mappingBeans = BeanFactoryUtils.beansOfTypeIncludingAncestors(
				context, HandlerMapping.class, true, false);
		ArrayList<HandlerMapping> mappings = new ArrayList<>(mappingBeans.values());
        // 按照Ordered接口排序
		AnnotationAwareOrderComparator.sort(mappings);
        // 赋值给成员变量handlerMappings，处理请求的映射关系
		this.handlerMappings = Collections.unmodifiableList(mappings);
		// 通过IOC容器获取所有类型为HandlerAdapter的Bean
		Map<String, HandlerAdapter> adapterBeans = BeanFactoryUtils.beansOfTypeIncludingAncestors(
				context, HandlerAdapter.class, true, false);
		// 赋值给成员变量handlerAdapters，执行请求对应的方法
		this.handlerAdapters = new ArrayList<>(adapterBeans.values());
		AnnotationAwareOrderComparator.sort(this.handlerAdapters);
		// 通过IOC容器获取所有类型为HandlerResultHandler的Bean
		Map<String, HandlerResultHandler> beans = BeanFactoryUtils.beansOfTypeIncludingAncestors(
				context, HandlerResultHandler.class, true, false);
		// 赋值给成员变量resultHandlers，处理返回结果时使用
		this.resultHandlers = new ArrayList<>(beans.values());
		AnnotationAwareOrderComparator.sort(this.resultHandlers);
	}

	@Override
	public Mono<Void> handle(ServerWebExchange exchange) {
		if (this.handlerMappings == null) {
			return createNotFoundError();
		}
		return Flux.fromIterable(this.handlerMappings) // 迭代handlerMappings
				.concatMap(mapping -> mapping.getHandler(exchange))//通过handlerMappings获取可执行的HandlerFunction
				.next()
				.switchIfEmpty(createNotFoundError()) //如果handlerFunction为空，则报错
				.flatMap(handler -> invokeHandler(exchange, handler)) // 执行对应的handlerFunction
				.flatMap(result -> handleResult(exchange, result));// 处理handlerFunction的返回结果
	}

	private Mono<HandlerResult> invokeHandler(ServerWebExchange exchange, Object handler) {
		if (this.handlerAdapters != null) {
            // 循环已有的handlerAdapter，类似于责任链模式
			for (HandlerAdapter handlerAdapter : this.handlerAdapters) {
                // 判断当前handlerAdapter是否可处理handlerFunction
				if (handlerAdapter.supports(handler)) {
                    // 执行对应的handlerFunction，返回值封装成handlerResult
					return handlerAdapter.handle(exchange, handler);
				}
			}
		}
		return Mono.error(new IllegalStateException("No HandlerAdapter: " + handler));
	}
	// 处理返回结果
	private Mono<Void> handleResult(ServerWebExchange exchange, HandlerResult result) {
        // 获取返回结果处理器resultHandler并处理返回结果
		return getResultHandler(result).handleResult(exchange, result)
				.checkpoint("Handler " + result.getHandler() + " [DispatcherHandler]")
				.onErrorResume(ex ->
						result.applyExceptionHandler(ex).flatMap(exResult -> {
							String text = "Exception handler " + exResult.getHandler() +
									", error=\"" + ex.getMessage() + "\" [DispatcherHandler]";
							return getResultHandler(exResult).handleResult(exchange, exResult).checkpoint(text);
						}));
	}

	private HandlerResultHandler getResultHandler(HandlerResult handlerResult) {
		if (this.resultHandlers != null) {
            // 循环已有的resultHandlers，类似于责任链模式
			for (HandlerResultHandler resultHandler : this.resultHandlers) {
                // 判断当前resultHandler是否可支持处理返回结果
				if (resultHandler.supports(handlerResult)) {
					return resultHandler;
				}
			}
		}
		throw new IllegalStateException("No HandlerResultHandler for " + handlerResult.getReturnValue());
	}
}
```

简单总结一下上面的处理流程，首先处理请求的入口是`handle(ServerWebExchange exchange)`方法，`ServerWebExchange`对象中包含一些请求的元数据，可以理解为`ServerWebExchange`就是`Servlet`规范中`HttpServletRequest`，`HttpServletResponse`，`HttpSession`的综合体。通过迭代handlerMappings获取请求对应的`HandlerFunction`（函数式）或者`HandlerMethod`（注解式），如果为空则封装成`ResponseStatusException`异常，如果不为空则继续往下走，执行方法`invokeHandler(ServerWebExchange exchange, Object handler)`，遍历所有的handlerAdapter，找到可处理handler的adapter。`handlerAdapter#support()`方法对于函数式端点编程模型来说，判断handler的类型是否是`HandlerFunction`，对于注解驱动编程模型来说，是判断handler的类型是否是`HandlerMethod`。

遍历resultHandlers找到可支持处理返回结果的resultHandler，使用`HttpMessageWriter`将数据写回客户端

### Functional Endpoints（函数式端点）处理流程

![图图图](C:\Users\cdshenlong1\Downloads\图图图.png)

接下来我们分析一个`RouterFunctionMapping`这个类，`RouterFunctionMapping`是Spring WebFlux初始化的一个Bean，有两个比较重要的属性routerFunction和messageReaders，routerFunction是用来保存请求路径（path）与执行方法（HandlerFunction）的映射关系。先来看看`RouterFunctionMapping`是怎么初始化的，看看下面这段在`WebFluxConfigurationSupport`的代码。

```java
@Bean
public ServerCodecConfigurer serverCodecConfigurer() {
    ServerCodecConfigurer serverCodecConfigurer = ServerCodecConfigurer.create();
    configureHttpMessageCodecs(serverCodecConfigurer);
    return serverCodecConfigurer;
}

@Bean
public RouterFunctionMapping routerFunctionMapping(ServerCodecConfigurer serverCodecConfigurer) {
    RouterFunctionMapping mapping = createRouterFunctionMapping();
    mapping.setOrder(-1);  // go before RequestMappingHandlerMapping
    mapping.setMessageReaders(serverCodecConfigurer.getReaders());
    configureAbstractHandlerMapping(mapping, getPathMatchConfigurer());
    return mapping;
}
```

我们可以看到，在这里初始化了两个bean，`ServerCodecConfigurer`和`RouterFunctionMapping`，而在初始化`RouterFunctionMapping`这个bean时，用到了`ServerCodecConfigurer#getReaders`方法的返回值来作为httpMessageReaders属性的值，所以我们先来看看`ServerCodecConfigurer`是怎么创建的。`ServerCodecConfigurer`是一个接口，里面有一个静态方法`#create()`，可以看下面的代码片段。

```java
static ServerCodecConfigurer create() {
    return CodecConfigurerFactory.create(ServerCodecConfigurer.class);
}
```

我们看到了`CodecConfigurerFactory`，此时我们就会猜想，应该是用工厂模式创建的ServerCodecConfigurer，再看这行代码的样子，感觉很像SPI的ServiceLoader模式，接着跟进去看。

```java
public static <T extends CodecConfigurer> T create(Class<T> ifc) {
    Class<?> impl = defaultCodecConfigurers.get(ifc);
    if (impl == null) {
        throw new IllegalStateException("No default codec configurer found for " + ifc);
    }
    return (T) BeanUtils.instantiateClass(impl);
}
```

从命名就可以看出，从Map里通过一个接口取出对应的实现类，然后再实例化这个实现类，而接口`ServerCodecConfigurer`的实现类只有一个，那就是`DefaultServerCodecConfigurer`，所以这里实例化的应该就是`DefaultServerCodecConfigurer`。

```java
public DefaultServerCodecConfigurer() {
    super(new ServerDefaultCodecsImpl());
}

ServerDefaultCodecsImpl() {
}

BaseDefaultCodecs() {
    initReaders();
    initWriters();
}
```

通过构造器传参以及继承关系，最终会调用到`BaseDefaultCodecs`的构造方法，会初始化相关的Reader和Writer，举例说明一下其中一种Reader类型，其它的大家可以下来自己去看。

```java
protected void initTypedReaders() {
    this.typedReaders.clear();
    if (!this.registerDefaults) {
        return;
    }
    addCodec(this.typedReaders, new DecoderHttpMessageReader<>(new ByteArrayDecoder()));
    addCodec(this.typedReaders, new DecoderHttpMessageReader<>(new ByteBufferDecoder()));
    addCodec(this.typedReaders, new DecoderHttpMessageReader<>(new DataBufferDecoder()));
    if (nettyByteBufPresent) {
        addCodec(this.typedReaders, new DecoderHttpMessageReader<>(new NettyByteBufDecoder()));
    }
    addCodec(this.typedReaders, new ResourceHttpMessageReader(new ResourceDecoder()));
    addCodec(this.typedReaders, new DecoderHttpMessageReader<>(StringDecoder.textPlainOnly()));
    if (protobufPresent) {
        addCodec(this.typedReaders, new DecoderHttpMessageReader<>(this.protobufDecoder != null ?
                                                                   (ProtobufDecoder) this.protobufDecoder : new ProtobufDecoder()));
    }
    addCodec(this.typedReaders, new FormHttpMessageReader());

    // client vs server..
    extendTypedReaders(this.typedReaders);
}
```

可以看出，会给HttpMessageReaders属性填充一些默认的Reader，包含各种解码器，如果ByteArray、ByteBuffer、Protobuf等等

我们再回过头去看看存在接口和实现类映射关系的Map是怎么初始化的，在`CodecConfigurerFactory`类中有一静态代码块，如下图。

```java
// CodecConfigurer的配置文件名
private static final String DEFAULT_CONFIGURERS_PATH = "CodecConfigurer.properties";
// 存在接口与实现类映射关系的map
private static final Map<Class<?>, Class<?>> defaultCodecConfigurers = new HashMap<>(4);
static {
    try {
        // 从classpath下从配置文件加载数据
        Properties props = PropertiesLoaderUtils.loadProperties(
            new ClassPathResource(DEFAULT_CONFIGURERS_PATH, CodecConfigurerFactory.class));
        for (String ifcName : props.stringPropertyNames()) {
            String implName = props.getProperty(ifcName);
            Class<?> ifc = ClassUtils.forName(ifcName, CodecConfigurerFactory.class.getClassLoader());
            Class<?> impl = ClassUtils.forName(implName, CodecConfigurerFactory.class.getClassLoader());
            defaultCodecConfigurers.put(ifc, impl);
        }
    }
    catch (IOException | ClassNotFoundException ex) {
        throw new IllegalStateException(ex);
    }
}
```

其实逻辑比较简单，通过在classpath下找一个名叫CodecConfigurer.properties的文件，文件内容是key-value形式，key表示接口名，value表示实现类名。

再来看看`RouterFunctionMapping`的bean创建过程，先简单实例化一个RouterFunctionMapping，设置他的优先级比RequestMappingHandlerMapping，这样可以保证RouterFunctionMapping先生效，然后设置RouterFunctionMapping的httpMessageReaders属性为serverCodecConfigurer的readers。

```java
@Bean
public RouterFunctionMapping routerFunctionMapping(ServerCodecConfigurer serverCodecConfigurer) {
    RouterFunctionMapping mapping = createRouterFunctionMapping();
    // 保证RouterFunctionMapping的优先级比RequestMappingHandlerMapping高
    mapping.setOrder(-1);
    // 初始化httpMessageReaders
    mapping.setMessageReaders(serverCodecConfigurer.getReaders());
    // 设置一些跨域配置
    configureAbstractHandlerMapping(mapping, getPathMatchConfigurer());
    return mapping;
}
```

接下来看看`RouterFunctionMapping`的Bean生命周期`afterPropertiesSet()`方法

```java
public void afterPropertiesSet() throws Exception {
    // 如果messageReaders为空，则重新初始化一次，对于SpringBoot项目来说这里不可能为空
    if (CollectionUtils.isEmpty(this.messageReaders)) {
        ServerCodecConfigurer codecConfigurer = ServerCodecConfigurer.create();
        this.messageReaders = codecConfigurer.getReaders();
    }
    if (this.routerFunction == null) {
        // 初始化routerFunction
        initRouterFunctions();
    }
    if (this.routerFunction != null) {
        // 设置解析路径模式
        RouterFunctions.changeParser(this.routerFunction, getPathPatternParser());
    }
}
protected void initRouterFunctions() {
    // 获取所有的RouterFunction
    List<RouterFunction<?>> routerFunctions = routerFunctions();
    // 将所有的RouterFunction聚合成一个，有点像是链表
    this.routerFunction = routerFunctions.stream().reduce(RouterFunction::andOther).orElse(null);
    logRouterFunctions(routerFunctions);
}
private List<RouterFunction<?>> routerFunctions() {
    // 获取IOC容器中所有RouterFunction类型的bean
    List<RouterFunction<?>> functions = obtainApplicationContext()
        .getBeanProvider(RouterFunction.class)
        .orderedStream()
        .map(router -> (RouterFunction<?>)router)
        .collect(Collectors.toList());
    return (!CollectionUtils.isEmpty(functions) ? functions : Collections.emptyList());
}
// 打印所有RouterFunction的路径信息
private void logRouterFunctions(List<RouterFunction<?>> routerFunctions) {
    if (logger.isDebugEnabled()) {
        int total = routerFunctions.size();
        String message = total + " RouterFunction(s) in " + formatMappingName();
        if (logger.isTraceEnabled()) {
            if (total > 0) {
                routerFunctions.forEach(routerFunction -> logger.trace("Mapped " + routerFunction));
            }
            else {
                logger.trace(message);
            }
        }
        else if (total > 0) {
            logger.debug(message);
        }
    }
}
```

主要逻辑是初始化routerFunction属性，将IOC容器里所有类型为RouterFunction的bean全部聚合成一个RouterFunction，模式有点类似于链表模式。

接下来看一下RouterFunctionMapping是怎么查找合适的RouterFunction的。

```java
protected Mono<?> getHandlerInternal(ServerWebExchange exchange) {
    if (this.routerFunction != null) {
        // 将ServerWebExchange转换成ServerRequest
        ServerRequest request = ServerRequest.create(exchange, this.messageReaders);
        // routerFunction为DifferentComposedRouterFunction类型
        return this.routerFunction.route(request)
            .doOnNext(handler -> setAttributes(exchange.getAttributes(), request, handler));
    }
    else {
        return Mono.empty();
    }
}
// DifferentComposedRouterFunction中的route方法
public Mono<HandlerFunction<ServerResponse>> route(ServerRequest request) {
    // 遍历每个RouterFunction，这里类型为DefaultRouterFunction，找到符合条件的
    return Flux.concat(this.first.route(request), Mono.defer(() -> this.second.route(request)))
        // Mono不为空的第一个元素
        .next()
        .map(this::cast);
}
// DefaultRouterFunction中的route方法
public Mono<HandlerFunction<T>> route(ServerRequest request) {
    // 根据请求元数据断言
    if (this.predicate.test(request)) {
        if (logger.isTraceEnabled()) {
            String logPrefix = request.exchange().getLogPrefix();
            logger.trace(logPrefix + String.format("Matched %s", this.predicate));
        }
        return Mono.just(this.handlerFunction);
    }
    else {
        return Mono.empty();
    }
}
```

其实就是遍历一个一个的RouterFunction，根据请求元数据进行断言，判断符合条件的RouterFunction，如果符合，就返回HandlerFunction，否则就返回空的Mono，`Flux.concat(...).next()`语法会获取第一个不为空的Mono。

接下来就是通过`HandlerAdapter`执行具体的方法

```java
private Mono<HandlerResult> invokeHandler(ServerWebExchange exchange, Object handler) {
    if (this.handlerAdapters != null) {
        for (HandlerAdapter handlerAdapter : this.handlerAdapters) {
            // 遍历每个handlerAdapter，判断是否支持处理handler类型
            if (handlerAdapter.supports(handler)) {
                // 直接处理
                return handlerAdapter.handle(exchange, handler);
            }
        }
    }
    return Mono.error(new IllegalStateException("No HandlerAdapter: " + handler));
}

public boolean supports(Object handler) {
    // 判断handler的类型是否是HandlerFunction
    return handler instanceof HandlerFunction;
}

public Mono<HandlerResult> handle(ServerWebExchange exchange, Object handler) {
    HandlerFunction<?> handlerFunction = (HandlerFunction<?>) handler;
    // 获取必须的请求参数
    ServerRequest request = exchange.getRequiredAttribute(RouterFunctions.REQUEST_ATTRIBUTE);
    // 执行业务方法
    return handlerFunction.handle(request)
        .map(response -> new HandlerResult(handlerFunction, response, HANDLER_FUNCTION_RETURN_TYPE));
}
```





### Annotated Controllers（注解驱动）处理流程

![图图图 (2)](C:\Users\cdshenlong1\Downloads\图图图 (2).png)

