/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.event;

import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;

/**
 * {@link GenericApplicationListener} adapter that determines supported event types
 * through introspecting the generically declared type of the target listener.
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 3.0
 * @see org.springframework.context.ApplicationListener#onApplicationEvent
 */
public class GenericApplicationListenerAdapter implements GenericApplicationListener, SmartApplicationListener {

	private final ApplicationListener<ApplicationEvent> delegate;

	private final ResolvableType declaredEventType;


	/**
	 * Create a new GenericApplicationListener for the given delegate.
	 * @param delegate the delegate listener to be invoked
	 */
	@SuppressWarnings("unchecked")
	public GenericApplicationListenerAdapter(ApplicationListener<?> delegate) {
		Assert.notNull(delegate, "Delegate listener must not be null");
		this.delegate = (ApplicationListener<ApplicationEvent>) delegate;
		//解析监听器申明的事件类型
		this.declaredEventType = resolveDeclaredEventType(this.delegate);
	}


	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		this.delegate.onApplicationEvent(event);
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean supportsEventType(ResolvableType eventType) {
		if (this.delegate instanceof SmartApplicationListener) {
			Class<? extends ApplicationEvent> eventClass = (Class<? extends ApplicationEvent>) eventType.getRawClass(); //获取时间类型Class
			return ((SmartApplicationListener) this.delegate).supportsEventType(eventClass);  //关键 supportsEventType
		}
		else {
			return (this.declaredEventType == null || this.declaredEventType.isAssignableFrom(eventType));  //执行该步骤，匹配泛型中的事件类型
		}
	}

	/**
	 * 判断是否支持指定的事件
	 * @param eventType
	 * @return
	 */
	@Override
	public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
		return supportsEventType(ResolvableType.forType(eventType));
	}

	@Override
	public boolean supportsSourceType(Class<?> sourceType) {
		if (this.delegate instanceof SmartApplicationListener) {
			return ((SmartApplicationListener) this.delegate).supportsSourceType(sourceType);
		}
		else {
			return true;
		}
	}

	@Override
	public int getOrder() {
		return (this.delegate instanceof Ordered ? ((Ordered) this.delegate).getOrder() : Ordered.LOWEST_PRECEDENCE);
	}


    /**
     * GenericTypeResolver#resolveTypeArgument实现类似功能，在低版本的Spring boot中用的是该方法
     * @param listenerType
     * @return
     */
	static ResolvableType resolveDeclaredEventType(Class<?> listenerType) {
		ResolvableType resolvableType = ResolvableType.forClass(listenerType).as(ApplicationListener.class);
		if (resolvableType == null || !resolvableType.hasGenerics()) {
			return null;
		}
		return resolvableType.getGeneric();//获取泛型类型，不传递参数，默认获取第一个 ，例如 ApplicationListener<ApplicationStartedEvent>  则获取到ApplicationStartedEvent
	}

	private static ResolvableType resolveDeclaredEventType(ApplicationListener<ApplicationEvent> listener) {
		ResolvableType declaredEventType = resolveDeclaredEventType(listener.getClass());
		if (declaredEventType == null || declaredEventType.isAssignableFrom(
				ResolvableType.forClass(ApplicationEvent.class))) {  //执行步骤二，为ApplicationEvent子类

			Class<?> targetClass = AopUtils.getTargetClass(listener);
			if (targetClass != listener.getClass()) {
				declaredEventType = resolveDeclaredEventType(targetClass);
			}
		}
		return declaredEventType;
	}

}
