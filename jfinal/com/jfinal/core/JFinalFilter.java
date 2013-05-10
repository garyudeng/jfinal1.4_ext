/**
 * Copyright (c) 2011-2012, James Zhan 詹波 (jfinal@126.com).
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

package com.jfinal.core;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.jfinal.config.Constants;
import com.jfinal.config.JFinalConfig;
import com.jfinal.handler.Handler;
import com.jfinal.kit.StringKit;
import com.jfinal.log.Logger;
import com.jfinal.render.ViewType;

/**
 * JFinal framework filter
 */
public final class JFinalFilter implements Filter {
	
	private Handler handler;
	private String encoding;
	private JFinalConfig jfinalConfig;
	private Constants constants;
	private static final JFinal jfinal = JFinal.me();
	private static Logger log;
	private int contextPathLength;
	
	public void init(FilterConfig filterConfig) throws ServletException {
		
		createJFinalConfig(filterConfig.getInitParameter("configClass"));
		if (jfinal.init(jfinalConfig, filterConfig.getServletContext()) == false)
			throw new RuntimeException("JFinal init error!");
		
		
		//获取初始化参数
		encoding=filterConfig.getInitParameter("encoding");
		
		String devMode=filterConfig.getInitParameter("devMode");
		String urlParaSeparator=filterConfig.getInitParameter("urlParaSeparator");
		String viewType=filterConfig.getInitParameter("viewType");
		String autoConfig=filterConfig.getInitParameter("autoConfig");
		String maxPostSize=filterConfig.getInitParameter("maxPostSize");
		
		handler = jfinal.getHandler();
		constants = Config.getConstants();
		
		if(devMode!=null) constants.setDevMode(StringKit.toBoolean(devMode));
		if(encoding!=null) constants.setEncoding(encoding);
		if(urlParaSeparator!=null) constants.setUrlParaSeparator(urlParaSeparator);
		if(viewType!=null) constants.setViewType(ViewType.getViewType(viewType));
		if(autoConfig!=null) constants.setAutoConfig(StringKit.toBoolean(autoConfig));
		if(maxPostSize!=null) constants.setMaxPostSize(Integer.parseInt(maxPostSize));
		
		encoding = constants.getEncoding();
		jfinalConfig.afterJFinalStart();
		
		String contextPath = filterConfig.getServletContext().getContextPath();
		contextPathLength = (contextPath == null || "/".equals(contextPath) ? 0 : contextPath.length());
	}
	
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
		
		HttpServletRequest request = (HttpServletRequest)req;
		HttpServletResponse response = (HttpServletResponse)res;
		request.setCharacterEncoding(encoding);
		
		String target = request.getRequestURI();
		if (contextPathLength != 0)
			target = target.substring(contextPathLength);
		
		boolean[] isHandled = {false};
		try {
			handler.handle(target, request, response, isHandled);
		}
		catch (Exception e) {
			if (log.isErrorEnabled()) {
				String qs = request.getQueryString();
				log.error(qs == null ? target : target + "?" + qs, e);
			}
		}
		
		if (isHandled[0] == false)
			chain.doFilter(request, response);
	}
	
	public void destroy() {
		jfinalConfig.beforeJFinalStop();
		jfinal.stopPlugins();
	}
	
	private void createJFinalConfig(String configClass) {
		if (configClass == null)
			configClass=JFinal.DEFAULT_JFINAL_CFG;
		
		try {
			Object temp = Class.forName(configClass).newInstance();
			if (temp instanceof JFinalConfig)
				jfinalConfig = (JFinalConfig)temp;
			else
				throw new RuntimeException("Can not create instance of class: " + configClass + ". Please check the config in web.xml");
		} catch (InstantiationException e) {
			throw new RuntimeException("Can not create instance of class: " + configClass, e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Can not create instance of class: " + configClass, e);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Class not found: " + configClass + ". Please config it in web.xml", e);
		}
	}
	
	static void initLogger() {
		log = Logger.getLogger(JFinalFilter.class);
	}
}
