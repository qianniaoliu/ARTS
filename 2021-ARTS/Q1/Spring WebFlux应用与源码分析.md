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

### Functional Endpoints（函数式端点）处理流程

![图图图](C:\Users\cdshenlong1\Downloads\图图图.png)

### Annotated Controllers（注解驱动）处理流程

## DispatcherHandler源码分析

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

简单总结一下上面的处理流程，首先处理请求的入口是`handle(ServerWebExchange exchange)`方法，`ServerWebExchange`对象中包含一些请求的元数据，可以理解为`ServerWebExchange`就是`Servlet`规范中`HttpServletRequest`，`HttpServletResponse`，`HttpSession`的综合体。通过迭代handlerMappings获取请求对应的`HandlerFunction`，如果为空则封装成`ResponseStatusException`异常，如果不为空则继续往下走，执行方法`invokeHandler(ServerWebExchange exchange, Object handler)`，遍历所有的handlerAdapter，找到可处理handler的adapter。`handlerAdapter#support()`方法对于函数式端点编程模型来说，判断handler的类型是否是`HandlerFunction`，对于注解驱动编程模型来说，是判断handler的类型是否是`HandlerMethod`。

最后通过遍历resultHandlers找到

