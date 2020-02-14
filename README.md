# SpringMVC-shiro-cas-sso
shiro + cas sso 单点登录

## 项目目录
- cas-overlay-template

        cas服务器
- SpringMVC-shiro-cas

        shiro客户端
- table

        数据库表
https://blog.csdn.net/JsongNeu/article/details/104301042

项目框架
SpringMVC
使用cas做单点登录
shiro做授权管理
## 搭建cas server

[cas服务器搭建 md5+盐加密](https://blog.csdn.net/JsongNeu/article/details/104227262)
## 客户端
### pom.xml
需要的依赖
```xml
        <!--spring mvc-->
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-webmvc</artifactId>
            <version>${spring.version}</version>
        </dependency>

        <!--        spring mvc end-->

        <dependency>
            <groupId>org.apache.shiro</groupId>
            <artifactId>shiro-core</artifactId>
            <version>${shiro.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.shiro</groupId>
            <artifactId>shiro-cas</artifactId>
            <version>${shiro.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.shiro</groupId>
            <artifactId>shiro-spring</artifactId>
            <version>${shiro.version}</version>
        </dependency>
```
依赖版本
```xml
		<spring.version>4.3.18.RELEASE</spring.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <maven.compiler.source>1.7</maven.compiler.source>
        <maven.compiler.target>1.7</maven.compiler.target>
        <shiro.version>1.4.0</shiro.version>
```

### 继承CasRealm
CasRealm里定义了cas身份认证和权限鉴定的方法，继承CasRealm类，重写我们自己的认证 和 鉴权方法
- doGetAuthenticationInfo方法
> 当用户登录认证时进入改方法
- doGetAuthorizationInfo方法
> 当访问需要授权的资源时，进入该方法
```java
package com.jsong.wiki.shiro.service;

import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.cas.CasRealm;
import org.apache.shiro.subject.PrincipalCollection;

public class ShiroCasRealm extends CasRealm {

    /**
     * cas 认证
     * @param token
     * @return
     */
    public AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token){
        AuthenticationInfo authc = super.doGetAuthenticationInfo(token);
        return authc;
    }

    /**
     * 设置角色 权限信息
     * @param principals
     * @return
     */
    @Override
    public AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        String account = (String) principals.getPrimaryPrincipal();
        System.out.println(account);
        SimpleAuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo();
        return authorizationInfo;
    }
}

```

### shiro-context.xml
```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">

    <!-- 自动注入properties属性文件 -->
    <bean id="propertyConfigurer" class="org.springframework.beans.factory.config.PropertyPlaceholderConfigurer" >
        <property name="locations">
            <list>
                <value>classpath:conf/shiro.properties</value>
            </list>
        </property>
    </bean>
    <!-- 配置shiroFilter 要在web.xml中配置DelegatingFilterProxy过滤器，初始化targetBeanName -->
    <bean id="shiroFilter" class="org.apache.shiro.spring.web.ShiroFilterFactoryBean">
        <!-- 注入securityManager -->
        <property name="securityManager" ref="securityManager"/>
        <!-- 设置登录URL,当用户未认证,就会自动重定向到登录URL -->
        <property name="loginUrl" value="${shiro.loginUrl}"/>
        <!-- 将Filter添加到Shiro过滤器链中,用于对资源设置权限 -->
        <property name="filters">
            <map>
                <entry key="casFilter" value-ref="casFilter"/>
                <entry key="logoutFilter" value-ref="logoutFilter"/>
            </map>
        </property>
        <!-- 配置哪些请求需要受保护,以及访问这些页面需要的权限 -->
        <property name="filterChainDefinitions">
            <value>
                /logout = logoutFilter
                /jsong = casFilter
<!--                访问所有的url都需要授权 -->
                /** = authc
<!--                /test/** = authc-->
<!--                /test/** = roles,user,perms-->
            </value>
        </property>
    </bean>

    <!-- 单点登录过滤器 -->
    <bean id="casFilter" class="org.apache.shiro.cas.CasFilter">
        <!-- 配置验证成功时跳转的URL -->
        <property name="successUrl" value="${shiro.successUrl}"/>
        <!-- 配置验证错误时跳转的URL -->
        <property name="failureUrl" value="${shiro.failureUrl}"/>
    </bean>

    <!--单点登出过滤器-->
    <bean id="logoutFilter" class="org.apache.shiro.web.filter.authc.LogoutFilter">
        <!-- 退出时重定向的URL -->
        <property name="redirectUrl" value="${shiro.logoutUrl}"/>
    </bean>

    <!-- 注册自定义CasRealm -->
    <bean id="casRealm" class="com.jsong.wiki.shiro.service.ShiroCasRealm">
        <!-- cas服务端地址前缀,作为ticket校验 -->
        <property name="casServerUrlPrefix" value="${shiro.cas.serverUrlPrefix}"/>
        <!-- 应用服务地址,用来接收CAS服务端的票据，服务地址要和shiro.loginUrl service=? 后面的地址一致 -->
        <property name="casService" value="${shiro.cas.service}"/>
    </bean>

    <!-- 配置securityManager -->
    <bean id="securityManager" class="org.apache.shiro.web.mgt.DefaultWebSecurityManager">
        <property name="subjectFactory" ref="casSubjectFactory"/>
        <property name="realm" ref="casRealm"/>
    </bean>

    <bean id="casSubjectFactory" class="org.apache.shiro.cas.CasSubjectFactory"></bean>

    <!-- 配置lifecycleBeanPostProcessor,shiro bean的生命周期管理器,可以自动调用Spring IOC容器中shiro bean的生命周期方法(初始化/销毁)-->
    <bean id="lifecycleBeanPostProcessor" class="org.apache.shiro.spring.LifecycleBeanPostProcessor"/>

    <!-- 为了支持Shiro的注解需要定义DefaultAdvisorAutoProxyCreator和AuthorizationAttributeSourceAdvisor两个bean -->

    <!-- 配置DefaultAdvisorAutoProxyCreator,必须配置了lifecycleBeanPostProcessor才能使用 -->
    <bean class="org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator"  depends-on="lifecycleBeanPostProcessor"/>

    <!-- 配置AuthorizationAttributeSourceAdvisor -->
    <bean class="org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor">
        <property name="securityManager" ref="securityManager"/>
    </bean>

</beans>
```

### applicationContext.xml
导入总体的配置文件
```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:aop="http://www.springframework.org/schema/aop"
       xmlns:context="http://www.springframework.org/schema/context"
       xmlns:mvc="http://www.springframework.org/schema/mvc"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd http://www.springframework.org/schema/aop http://www.springframework.org/schema/aop/spring-aop.xsd http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd http://www.springframework.org/schema/mvc http://www.springframework.org/schema/mvc/spring-mvc.xsd">
    <!--包扫描-->
    <context:component-scan base-package="com.jsong.wiki.shiro"/>
    <!--    注释-->
    <mvc:annotation-driven/>

    <!--spring mvc-->
    <import resource="spring-mvc.xml"></import>
    <!--    shiro 配置-->
    <import resource="shiro-context.xml"/>
    <!--    -->
    <!--    <aop:config proxy-target-class="true"></aop:config>-->
    <!--    <bean class="org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor">-->
    <!--        <property name="securityManager" ref="securityManager"/>-->
    <!--    </bean>-->
</beans>  
   
```

### web.xml
配置web.xml serlet拦截器和filter
```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xmlns="http://java.sun.com/xml/ns/javaee" xmlns:jsp="http://java.sun.com/xml/ns/javaee/jsp"
         xmlns:web="http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd"
         xsi:schemaLocation="http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd"
         id="WebApp_ID" version="2.5">
    <display-name>Archetype Created Web Application</display-name>
    <servlet>
        <servlet-name>dispatcher</servlet-name>
        <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
        <init-param>
            <param-name>contextConfigLocation</param-name>
            <param-value>classpath:conf/applicationContext.xml</param-value>
        </init-param>
        <!--    标记容器是否在启动的时候就加载这个servlet
        当是一个负数时或者没有指定时，则指示容器在该servlet被选择时才加载。
        正数的值越小，启动该servlet的优先级越高
        -->
        <load-on-startup>1</load-on-startup>
    </servlet>

    <servlet-mapping>
        <servlet-name>dispatcher</servlet-name>
        <url-pattern>/index</url-pattern>
    </servlet-mapping>
    <servlet-mapping>
        <servlet-name>dispatcher</servlet-name>
        <url-pattern>/</url-pattern>
    </servlet-mapping>

    <filter>
        <filter-name>shiroFilter</filter-name>
        <filter-class>org.springframework.web.filter.DelegatingFilterProxy</filter-class>
        <init-param>
            <param-name>targetBeanName</param-name>
            <param-value>shiroFilter</param-value>
        </init-param>
    </filter>
    <filter-mapping>
        <filter-name>shiroFilter</filter-name>
        <url-pattern>/*</url-pattern>
    </filter-mapping>

</web-app>

```

### controller
```java
package com.jsong.wiki.shiro.controller;

import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/shiro")
public class ShiroController {


    @RequiresRoles("admin")
    @RequestMapping("/hello")
    public String getHello() {
        return "Hello World";
//        return "../index";
    }
}

```

[关于不进入doGetAuthorizationInfo方法问题](https://blog.csdn.net/JsongNeu/article/details/104295245)