# FJDBC
fritz's wrapper around spring framework jdbc

If you like Spring DATA JPA, but you still would like to use Spring Framework's JDBC (SimpleJDBC) check this out.

## Configuration:

Add this bean definition in your Spring's XML configuration:

    <bean id="databaseProcessor" class="hr.fritz.fjdbc.DatabaseProcessor">
      <constructor-arg value="PLACE_HERE_YOUR_REPOSITORY_PACKAGE_NAME" />
      <constructor-arg ref="dataSource" />
    </bean> 

## Usage:

Create interface like this, for example:

    @DatabaseRepository
    public interface ProductRepository {

      @DatabaseCall(sproc = "dbo.Product_Get", domain = Product.class)
  	  public Product get(Map<String, Object> inParam) throws RecordNotFoundException;

      @DatabaseCall(sproc = "dbo.Product_GetAllPaged", domain = Product.class)
      public PageListResult<Product> getAllPaged(Map<String, Object> inParam);

      @DatabaseCall(sproc = "dbo.Product_Create", domain = Product.class)
      public Product create(Map<String, Object> inParam);

      @DatabaseCall(sproc = "dbo.Product_Delete")
      public void delete(Map<String, Object> inParam);

      @DatabaseCall(sproc = "dbo.Search_ProductByName", domain = Product.class)
      public List<Product> searchByName(Map<String, Object> inParam);

      @DatabaseCall(sproc = "dbo.Search_ProductByNumber", domain = Product.class)
      public List<Product> searchByNumber(Map<String, Object> inParam);
  
      @DatabaseCall(sproc = "dbo.Product_Code__IsNameUnique", column="isUnique", useCustomExceptionTranslation = true)
      public Boolean isCityNameUnique(Map<String, Object> inParam);

    }
    
FJDBC will create class instance based on this interface. You always pass arguments to stored procedure in Map, even single argument. 

Difference between PageListResult and List is that PageListResult requires two result sets from stored procedure where second contains maxPageNo. 

In your controller / service class you just have te add your repository interface (class).

    @Service
    public class ProductService {
	
      @Autowired
      private ProductRepository productRepository;
  
      ...
  
    }
