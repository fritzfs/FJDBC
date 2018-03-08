package hr.fritz.fjdbc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;

public class DatabaseInvocationHandler implements InvocationHandler {

	private DataSource dataSource;
	private Map<String, DatabaseMethod> methods = new HashMap<String, DatabaseMethod>();

	public DatabaseInvocationHandler(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		Map<String, Object> param = (Map<String, Object>) args[0];
		if (method.isAnnotationPresent(DatabaseCall.class)) {
			String methodName = method.getName();
			if (methods.containsKey(methodName)) {
				if (method.getAnnotation(DatabaseCall.class).domain() != Object.class) {
					if (method.getReturnType() == List.class) {
						return methods.get(methodName).invokeList(param);
					} else if (method.getReturnType() == PageListResult.class) {
						return methods.get(methodName).invokePageList(param);
					} else {
						return methods.get(methodName).invoke(param);
					}
				} else {
					if (method.getReturnType() == List.class) {
						return methods.get(methodName).invokeColumnList(param);
					} else if (method.getReturnType() == PageListResult.class) {
						return methods.get(methodName).invokeColumnPageList(param);
					} else if (method.getReturnType().equals(Void.TYPE)) {
						methods.get(methodName).invokeExecute(param);
					} else {
						return methods.get(methodName).invokeColumn(param);
					}
				}
			}
		}
		return null;
	}

	/**
	 * Prepare methods before invoke(). Build SimpleJdbcCall ...
	 * 
	 * @param clazz
	 */
	public void setup(Class<?> clazz) {
		for (Method method : clazz.getMethods()) {
			if (method.isAnnotationPresent(DatabaseCall.class)) {
				String methodName = method.getName();
				if (!methods.containsKey(methodName)) {
					String sproc = method.getAnnotation(DatabaseCall.class).sproc();

					Class<?> domain = method.getAnnotation(DatabaseCall.class).domain();

					DatabaseMethod databaseMethod;

					if (domain != Object.class) {
						databaseMethod = new DatabaseMethod(dataSource, method, sproc, domain);
					} else {
						String column = method.getAnnotation(DatabaseCall.class).column();
						if (column.equals("") && !method.getReturnType().equals(Void.TYPE)) {
							throw new RuntimeException("Unable to determine column name for stored procedure: " + methodName);
						}
						databaseMethod = new DatabaseMethod(dataSource, method, sproc, column);
					}

					methods.put(methodName, databaseMethod);
				}
			}
		}
	}

	public class DatabaseMethod {

		private String methodName;
		private String column;
		private boolean returnNullIfEmpty;
		private SimpleJdbcCall call;

		private DatabaseMethod(DataSource dataSource, Method method, String sproc) {
			if (!sproc.contains(".")) {
				throw new RuntimeException("Invalid sproc name! Missing dot between schema.procedure.");
			}

			this.methodName = method.getName();

			String schema = sproc.split("\\.")[0];
			String procedure = sproc.split("\\.")[1];

			call = new SimpleJdbcCall(dataSource);
			call.withSchemaName(schema);
			call.withProcedureName(procedure);

			returnNullIfEmpty = method.getAnnotation(DatabaseCall.class).returnNullIfEmpty();

			boolean customExceptionTranslation = method.getAnnotation(DatabaseCall.class).useCustomExceptionTranslation();
			if (customExceptionTranslation) {
				call.getJdbcTemplate().setExceptionTranslator(new MsSqlErrorCodesTranslator());
			}
		}

		public DatabaseMethod(DataSource dataSource, Method method, String sproc, String column) {
			this(dataSource, method, sproc);
			this.column = column;
		}

		public DatabaseMethod(DataSource dataSource, Method method, String sproc, Class<?> domain) {
			this(dataSource, method, sproc);
			call.returningResultSet("#result-set-1", BeanPropertyRowMapper.newInstance(domain));
		}

		public Object invoke(Map<String, Object> param) {
			Map<String, Object> map = call.execute(param);
			List<Object> list = (List<Object>) map.get("#result-set-1");
			if (list == null || list.size() == 0) {
				if (returnNullIfEmpty) {
					return null;
				} else {
					throw new RecordNotFoundException();
				}
			} else if (list.size() > 1) {
				throw new RuntimeException("Stored procedure returns more than one row: " + methodName);
			}
			return list.get(0);
		}

		public Object invokeColumn(Map<String, Object> param) {
			Map<String, Object> map = call.execute(param);
			List<Map<String, Object>> list = (List<Map<String, Object>>) map.get("#result-set-1");
			if (list == null || list.size() == 0) {
				if (returnNullIfEmpty) {
					return null;
				} else {
					throw new RecordNotFoundException();
				}
			} else if (list.size() > 1) {
				throw new RuntimeException("Stored procedure returns more than one row: " + methodName);
			}
			Map<String, Object> map2 = list.get(0);
			return map2.get(column);
		}

		public List<Object> invokeList(Map<String, Object> param) {
			Map<String, Object> map = call.execute(param);
			List<Object> list = (List<Object>) map.get("#result-set-1");
			if (list == null) {
				throw new RuntimeException("Stored procedure returns null: " + methodName);
			}
			return list;
		}

		public List<Object> invokeColumnList(Map<String, Object> param) {
			List<Object> result = new ArrayList<Object>();
			Map<String, Object> map = call.execute(param);
			List<Map<String, Object>> list = (List<Map<String, Object>>) map.get("#result-set-1");
			if (list == null) {
				throw new RuntimeException("Stored procedure returns null: " + methodName);
			}
			for (Map<String, Object> map2 : list) {
				result.add(map2.get(column));
			}
			return result;
		}

		public PageListResult<Object> invokePageList(Map<String, Object> param) {
			PageListResult result = new PageListResult();
			Map<String, Object> map = call.execute(param);
			List<Object> list = (List<Object>) map.get("#result-set-1");
			if (list == null) {
				throw new RuntimeException("Stored procedure returns null: " + methodName);
			}
			result.setData(list);
			List list2 = (List) map.get("#result-set-2");
			if (list2 == null || list2.size() == 0) {
				throw new RuntimeException("Stored procedure returns null or empty on maxPageNo: " + methodName);
			} else if (list2.size() > 1) {
				throw new RuntimeException("Stored procedure returns more than one row on maxPageNo: " + methodName);
			}
			Map map2 = (Map) list2.get(0);
			Integer maxPageNo = (Integer) map2.get("maxPageNo");
			result.setMaxPageNo(maxPageNo);
			return result;
		}

		public PageListResult<Object> invokeColumnPageList(Map<String, Object> param) {
			PageListResult result = new PageListResult();
			Map<String, Object> map = call.execute(param);
			List<Map<String, Object>> list = (List<Map<String, Object>>) map.get("#result-set-1");
			if (list == null) {
				throw new RuntimeException("Stored procedure returns null: " + methodName);
			}
			List<Object> resultList = new ArrayList<Object>();
			for (Map<String, Object> map2 : list) {
				resultList.add(map2.get(column));
			}
			result.setData(resultList);
			List list2 = (List) map.get("#result-set-2");
			if (list2 == null || list2.size() == 0) {
				throw new RuntimeException("Stored procedure returns null or empty on maxPageNo: " + methodName);
			} else if (list2.size() > 1) {
				throw new RuntimeException("Stored procedure returns more than one row on maxPageNo: " + methodName);
			}
			Map map2 = (Map) list2.get(0);
			Integer maxPageNo = (Integer) map2.get("maxPageNo");
			result.setMaxPageNo(maxPageNo);
			return result;
		}
		
		public void invokeExecute(Map<String, Object> param) {
			call.execute(param);
		}

		private String determineColumn(Method method) {
			String column = method.getAnnotation(DatabaseCall.class).column();
			if (column.equals("")) {
				if (methodName.startsWith("get")) {
					column = methodName.substring("get".length());
				} else {
					throw new RuntimeException("Unable to determine column name for stored procedure: " + methodName);
				}
			}
			return column;
		}

	}

}
