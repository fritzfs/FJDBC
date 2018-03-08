package hr.fritz.fjdbc;

import java.sql.SQLException;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;

/**
 * Microsoft SQL Servers throws exceptions under error code 50000 and they are
 * categorized as org.springframework.jdbc.UncategorizedSQLException. When
 * printing such exceptions you have messages such as:
 *  org.springframework.jdbc.UncategorizedSQLException:
 *  CallableStatementCallback; uncategorized SQLException for SQL [{call
 *  dbo.Pos_Activate(?, ?)}]; SQL state [S0001]; error code [50000]; Device with
 *  that hwid ? already exists; nested exception is
 *  com.microsoft.sqlserver.jdbc.SQLServerException: Device with that hwid ?
 *  already exists
 * 
 * If you use to get only SQLServerException use this translator.
 *
 */
public class MsSqlErrorCodesTranslator extends SQLErrorCodeSQLExceptionTranslator {

	@Override
	protected DataAccessException customTranslate(String task, String sql, SQLException sqlEx) {
		if (sqlEx.getErrorCode() == 50000) {
			return new DatabaseException(sqlEx.getMessage(), sqlEx);
		}
		return null;
	}

}
