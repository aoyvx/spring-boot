/*
 * Copyright 2012-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.endpoint.web.servlet;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.actuate.endpoint.web.EndpointLinksResolver;
import org.springframework.boot.actuate.endpoint.web.EndpointMapping;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.Link;
import org.springframework.boot.actuate.endpoint.web.servlet.WebMvcEndpointHandlerMapping.WebMvcEndpointHandlerMappingRuntimeHints;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.context.aot.BindingReflectionHintsRegistrar;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.HandlerMapping;

/**
 * A custom {@link HandlerMapping} that makes web endpoints available over HTTP using
 * Spring MVC.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 2.0.0
 */
@ImportRuntimeHints(WebMvcEndpointHandlerMappingRuntimeHints.class)
public class WebMvcEndpointHandlerMapping extends AbstractWebMvcEndpointHandlerMapping {

	private final EndpointLinksResolver linksResolver;

	/**
	 * Creates a new {@code WebMvcEndpointHandlerMapping} instance that provides mappings
	 * for the given endpoints.
	 * @param endpointMapping the base mapping for all endpoints
	 * @param endpoints the web endpoints
	 * @param endpointMediaTypes media types consumed and produced by the endpoints
	 * @param corsConfiguration the CORS configuration for the endpoints or {@code null}
	 * @param linksResolver resolver for determining links to available endpoints
	 * @param shouldRegisterLinksMapping whether the links endpoint should be registered
	 */
	public WebMvcEndpointHandlerMapping(EndpointMapping endpointMapping, Collection<ExposableWebEndpoint> endpoints,
			EndpointMediaTypes endpointMediaTypes, CorsConfiguration corsConfiguration,
			EndpointLinksResolver linksResolver, boolean shouldRegisterLinksMapping) {
		super(endpointMapping, endpoints, endpointMediaTypes, corsConfiguration, shouldRegisterLinksMapping);
		this.linksResolver = linksResolver;
		setOrder(-100);
	}

	@Override
	protected LinksHandler getLinksHandler() {
		return new WebMvcLinksHandler();
	}

	/**
	 * Handler for root endpoint providing links.
	 */
	class WebMvcLinksHandler implements LinksHandler {

		@Override
		@ResponseBody
		public Map<String, Map<String, Link>> links(HttpServletRequest request, HttpServletResponse response) {
			return Collections.singletonMap("_links",
					WebMvcEndpointHandlerMapping.this.linksResolver.resolveLinks(request.getRequestURL().toString()));
		}

		@Override
		public String toString() {
			return "Actuator root web endpoint";
		}

	}

	static class WebMvcEndpointHandlerMappingRuntimeHints implements RuntimeHintsRegistrar {

		private final BindingReflectionHintsRegistrar bindingRegistrar = new BindingReflectionHintsRegistrar();

		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			Method linksMethod = ReflectionUtils.findMethod(WebMvcLinksHandler.class, "links", HttpServletRequest.class,
					HttpServletResponse.class);
			Assert.state(linksMethod != null, "Unable to find 'links' method");
			hints.reflection().registerMethod(linksMethod);
			this.bindingRegistrar.registerReflectionHints(hints.reflection(), Link.class);
		}

	}

}
