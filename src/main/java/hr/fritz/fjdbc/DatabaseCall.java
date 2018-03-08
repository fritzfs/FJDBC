package hr.fritz.fjdbc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specify domain is POJO mapping is needed.
 * 
 * When not using domain, specify column.
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DatabaseCall {
	
	public String sproc();
	
	/**
	 * Specify domain if using List or PageListResult.
	 * 
	 * @return
	 */
	public Class<?> domain() default Object.class;
	
	/**
	 * If not using domain specify column.
	 * @return
	 */
	public String column() default "";
	
	/**
	 * Set to true if you wish to get null on empty result in single row methods.
	 * @return
	 */
	public boolean returnNullIfEmpty() default false;
	
	/**
	 * Set to true if you wish to use MsSqlErrorCodesTranslator.
	 */
	public boolean useCustomExceptionTranslation() default false;

}
