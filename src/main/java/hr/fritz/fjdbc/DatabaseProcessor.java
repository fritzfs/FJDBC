package hr.fritz.fjdbc;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.ClassUtils;

public class DatabaseProcessor implements BeanFactoryPostProcessor {

	protected final Log logger = LogFactory.getLog(getClass());

	private static final String DEFAULT_RESOURCE_PATTERN = "**/*.class";

	private ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();

	private MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(resourcePatternResolver);

	private Environment environment = new StandardEnvironment();

	private List<Object> proxies = new ArrayList<Object>();

	public DatabaseProcessor(String basePackage, DataSource dataSource) {
		try {
			List<Class<?>> classes = findInterfaces(basePackage);
			for (Class<?> clazz : classes) {
				DatabaseInvocationHandler invocationHandler = new DatabaseInvocationHandler(dataSource);
				Object proxy = Proxy.newProxyInstance(clazz.getClassLoader(), new Class[] { clazz }, invocationHandler);
				invocationHandler.setup(clazz);
				proxies.add(proxy);
			}
		} catch (Exception e) {
			logger.error("Unable to create database repository proxy.", e);
		}

	}

	/**
	 * Similar as
	 * ClassPathScanningCandidateComponentProvider.findCandidateComponents
	 * except that we're looking for interfaces with DatabaseRepository
	 * annotation.
	 * 
	 * @param basePackage
	 * @return list of interfaces which needs to be proxied
	 */
	private List<Class<?>> findInterfaces(String basePackage) {
		List<Class<?>> classes = new ArrayList<Class<?>>();
		try {
			String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + resolveBasePackage(basePackage) + "/" + DEFAULT_RESOURCE_PATTERN;
			Resource[] resources = resourcePatternResolver.getResources(packageSearchPath);
			for (Resource resource : resources) {
				if (resource.isReadable()) {
					MetadataReader metadataReader = this.metadataReaderFactory.getMetadataReader(resource);
					Class<?> clazz = Class.forName(metadataReader.getAnnotationMetadata().getClassName());
					if (clazz.isAnnotationPresent(DatabaseRepository.class)) {
						classes.add(clazz);
					}
				}
			}
		} catch (IOException e) {
			logger.error("Unable to scan base package for DatabaseRepository", e);
		} catch (ClassNotFoundException e) {
			logger.error("Unable to scan base package for DatabaseRepository", e);
		}
		return classes;
	}

	/**
	 * Stolen from ClassPathScanningCandidateComponentProvider.
	 * 
	 * Resolve the specified base package into a pattern specification for the
	 * package search path.
	 * <p>
	 * The default implementation resolves placeholders against system
	 * properties, and converts a "."-based package path to a "/"-based resource
	 * path.
	 * 
	 * @param basePackage
	 *            the base package as specified by the user
	 * @return the pattern specification to be used for package searching
	 */
	private String resolveBasePackage(String basePackage) {
		return ClassUtils.convertClassNameToResourcePath(this.environment.resolveRequiredPlaceholders(basePackage));
	}

	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		for (Object proxy : proxies) {
			String beanName = convertToCamelCase(proxy.getClass().getInterfaces()[0].getSimpleName());
			beanFactory.registerSingleton(beanName, proxy);
		}
	}

	private String convertToCamelCase(String name) {
		StringBuilder sb = new StringBuilder();
		sb.append(Character.toLowerCase(name.charAt(0)));
		sb.append(name.substring(1));
		return sb.toString();
	}

}
