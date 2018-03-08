package hr.fritz.fjdbc;

import java.util.List;

public class PageListResult<T> {
	
	private List<T> data;
	
	private Integer maxPageNo;

	public List<T> getData() {
		return data;
	}

	public void setData(List<T> data) {
		this.data = data;
	}

	public Integer getMaxPageNo() {
		return maxPageNo;
	}

	public void setMaxPageNo(Integer maxPageNo) {
		this.maxPageNo = maxPageNo;
	}		

}
