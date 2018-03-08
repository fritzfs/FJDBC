package hr.fritz.fjdbc;

import org.springframework.dao.DataAccessException;

public class DatabaseException extends DataAccessException {

	public DatabaseException(String message, Throwable cause) {
		super(message, cause);
	}

	@Override
	public String getMessage() {
		if (super.getRootCause() != null && super.getRootCause().getMessage() != null) {
			return super.getRootCause().getMessage();
		} else {
			return super.getMessage();
		}
	}	
	
}
