package umm3601.todo;

// import static com.mongodb.client.model.Filters.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
// import static org.junit.jupiter.api.Assertions.assertNotEquals;
// import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
// import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
// import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
// import java.util.Collections;
// import java.util.HashMap;
import java.util.List;
import java.util.Map;
// import java.util.stream.Collectors;

import org.bson.Document;
import org.bson.types.ObjectId;
// import org.eclipse.jetty.util.IO;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
// import org.mockito.ArgumentMatcher;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

// import com.fasterxml.jackson.core.JsonProcessingException;
// import com.fasterxml.jackson.databind.JsonMappingException;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.NotFoundResponse;
import io.javalin.json.JavalinJackson;
// import io.javalin.validation.BodyValidator;
// import io.javalin.validation.Validation;
// import io.javalin.validation.ValidationError;
// import io.javalin.validation.ValidationException;
// import io.javalin.validation.Validator;

import umm3601.user.Todo;
import umm3601.user.TodoController;

public class TodoControllerSpec {
  private TodoController todoController;

  //private MongoCollection<Document> todoDocuments;

  private ObjectId guadalupesId;

  private static MongoClient mongoClient;
  private static MongoDatabase database;

  private static JavalinJackson javalinJackson = new JavalinJackson();

  @Mock
  private Context ctx;

  @Captor
  private ArgumentCaptor<ArrayList<Todo>> todoArrayListCaptor;

  @Captor
  private ArgumentCaptor<Todo> todoCaptor;

  @Captor
  private ArgumentCaptor<Map<String, String>> mapCaptor;



  @BeforeAll
  static void setupAll() {
  String mongoAddr = System.getenv().getOrDefault("MONGO_ADDR", "localhost");

  mongoClient = MongoClients.create(
    MongoClientSettings.builder()
      .applyToClusterSettings(builder -> builder.hosts(Arrays.asList(new ServerAddress(mongoAddr))))
      .build());
    database = mongoClient.getDatabase("test");
  }

  @AfterAll
  static void teardown() {
    database.drop();
    mongoClient.close();
  }

  @BeforeEach
  void setupEach() throws IOException {
    MockitoAnnotations.openMocks(this);

    MongoCollection<Document> todoDocuments = database.getCollection("todos");
    todoDocuments.drop();
    List<Document> testTodos = new ArrayList<>();

    testTodos.add(
      new Document()
        .append("owner", "Chris")
        .append("status", true)
        .append("body", "UMM is great!")
        .append("category", "school"));
    testTodos.add(
      new Document()
        .append("owner", "Jamie")
        .append("status", true)
        .append("body", "Eat more (7) vegetables.")
        .append("category", "home"));
    testTodos.add(
      new Document()
        .append("owner", "Jamie")
        .append("status", false)
        .append("body", "Go to the gym.")
        .append("category", "leisure"));

      guadalupesId = new ObjectId();
      Document lupe = new Document()
        .append("_id", guadalupesId)
        .append("owner", "Guadalupe")
        .append("status", true)
        .append("body", "Finish Lab 2.")
        .append("category", "school");

    todoDocuments.insertMany(testTodos);
    todoDocuments.insertOne(lupe);

    todoController = new TodoController(database);
  }

  @Test
  void addsRoutes() {
    Javalin mockServer = mock(Javalin.class);
    todoController.addRoutes(mockServer);
    verify(mockServer, Mockito.atLeast(1)).get(any(), any());
    //verify(mockServer, Mockito.atLeastOnce()).post(any(), any());
    //verify(mockServer, Mockito.atLeastOnce()).patch(any(), any());
  }

  @Test
  void canGetAllTodos() throws IOException {
    when(ctx.queryParam("owner")).thenReturn(null);
    todoController.getTodos(ctx);

    verify(ctx).json(todoArrayListCaptor.capture());
    verify(ctx).status(HttpStatus.OK);

    assertEquals(database.getCollection("todos").countDocuments(),
     todoArrayListCaptor.getValue().size());
  }

  @Test
  void getTodoWithExistentId() throws IOException {
    String id = guadalupesId.toHexString();
    when(ctx.pathParam("id")).thenReturn(id);

    todoController.getTodo(ctx);

    verify(ctx).json(todoCaptor.capture());
    verify(ctx).status(HttpStatus.OK);
    assertEquals("Guadalupe", todoCaptor.getValue().owner);
    assertEquals(guadalupesId.toHexString(), todoCaptor.getValue()._id);
  }

  @Test
  void getTodoWithBadId() throws IOException {
    when(ctx.pathParam("id")).thenReturn("bad");

    Throwable exception = assertThrows(BadRequestResponse.class, () -> {
      todoController.getTodo(ctx);
    });

    assertEquals("The requested todo id was not a legal Mongo Object ID.", exception.getMessage());
  }

  @Test
  void getTodoWithNonexistentId() throws IOException {
    String id = "588935f5c668650dc77df581";
    when(ctx.pathParam("id")).thenReturn(id);

    Throwable exception = assertThrows(NotFoundResponse.class, () -> {
      todoController.getTodo(ctx);
    });

    assertEquals("The requested todo was not found", exception.getMessage());
  }

  @Test
  void getTodosWithNonNumericLimitThrowsError() throws IOException {
    when(ctx.queryParamMap()).thenReturn(Map.of("limit", List.of("abc")));
    when(ctx.queryParam("limit")).thenReturn("abc");

    BadRequestResponse exception = assertThrows(
      BadRequestResponse.class,
      () -> todoController.getTodos(ctx));

    assertEquals("Limit must be a non-negative integer", exception.getMessage());
  }

  @Test
  void getTodosWithNegativeLimit() throws IOException {
    when(ctx.queryParamMap()).thenReturn(Map.of("limit", List.of("-5")));
    when(ctx.queryParam("limit")).thenReturn("-5");

    BadRequestResponse exception = assertThrows(
      BadRequestResponse.class,
      () -> todoController.getTodos(ctx));

    assertEquals("Limit must be a non-negative integer", exception.getMessage());
  }

  @Test
  void getTodosThatContainRegularString() throws IOException {
    String searchString = "vegetables";
    when(ctx.queryParamMap()).thenReturn(Map.of("contains", List.of(searchString)));
    when(ctx.queryParam("contains")).thenReturn(searchString);

    todoController.getTodos(ctx);

    verify(ctx).json(todoArrayListCaptor.capture());
    verify(ctx).status(HttpStatus.OK);

    List<Todo> returnedTodos = todoArrayListCaptor.getValue();
    assertEquals(1, returnedTodos.size());
    assertEquals("Eat more (7) vegetables.", returnedTodos.get(0).body);
  }

  @Test
  void getTodosThatContainInteger() throws IOException {
    String searchString = "7";
    when(ctx.queryParamMap()).thenReturn(Map.of("contains", List.of(searchString)));
    when(ctx.queryParam("contains")).thenReturn(searchString);

    todoController.getTodos(ctx);

    verify(ctx).json(todoArrayListCaptor.capture());
    verify(ctx).status(HttpStatus.OK);

    List<Todo> returnedTodos = todoArrayListCaptor.getValue();
    assertEquals(1, returnedTodos.size());
    assertEquals("Eat more (7) vegetables.", returnedTodos.get(0).body);
  }

  @Test
  void getTodosThatContainStringWithSpecialCharacters() throws IOException {
    String searchString = "great!";
    when(ctx.queryParamMap()).thenReturn(Map.of("contains", List.of(searchString)));
    when(ctx.queryParam("contains")).thenReturn(searchString);

    todoController.getTodos(ctx);

    verify(ctx).json(todoArrayListCaptor.capture());
    verify(ctx).status(HttpStatus.OK);

    List<Todo> returnedTodos = todoArrayListCaptor.getValue();
    assertEquals(1, returnedTodos.size());
    assertEquals("UMM is great!", returnedTodos.get(0).body);
  }

  @Test
  void getTodosThatContainIsCaseInsensitive() throws IOException {
    String searchString = "gReAt!";
    when(ctx.queryParamMap()).thenReturn(Map.of("contains", List.of(searchString)));
    when(ctx.queryParam("contains")).thenReturn(searchString);

    todoController.getTodos(ctx);

    verify(ctx).json(todoArrayListCaptor.capture());
    verify(ctx).status(HttpStatus.OK);

    List<Todo> returnedTodos = todoArrayListCaptor.getValue();
    assertEquals(1, returnedTodos.size());
    assertEquals("UMM is great!", returnedTodos.get(0).body);
  }

  @Test
  void getTodosThatContainStringThatIsNotInAnyTodo() throws IOException {
    String searchString = "asldkfjalskdfj";
    when(ctx.queryParamMap()).thenReturn(Map.of("contains", List.of(searchString)));
    when(ctx.queryParam("contains")).thenReturn(searchString);

    todoController.getTodos(ctx);

    verify(ctx).json(todoArrayListCaptor.capture());
    verify(ctx).status(HttpStatus.OK);

    List<Todo> returnedTodos = todoArrayListCaptor.getValue();
    assertEquals(0, returnedTodos.size());
  }

  @Test
  void getTodosWithEmptyContainsParameter() throws IOException {
    String searchString = "";
    when(ctx.queryParamMap()).thenReturn(Map.of("contains", List.of(searchString)));
    when(ctx.queryParam("contains")).thenReturn(searchString);

    todoController.getTodos(ctx);

    verify(ctx).json(todoArrayListCaptor.capture());
    verify(ctx).status(HttpStatus.OK);

    List<Todo> returnedTodos = todoArrayListCaptor.getValue();
    assertEquals(database.getCollection("todos").countDocuments(), returnedTodos.size());
  }

  @Test
  void getTodosOwnedByChris() throws IOException {
    String owner = "Chris";
    when(ctx.queryParamMap()).thenReturn(Map.of("owner", List.of(owner)));
    when(ctx.queryParam("owner")).thenReturn(owner);

    todoController.getTodos(ctx);

    verify(ctx).json(todoArrayListCaptor.capture());
    verify(ctx).status(HttpStatus.OK);

    List<Todo> returnedTodos = todoArrayListCaptor.getValue();
    assertEquals(1, returnedTodos.size());
    assertEquals("Chris", returnedTodos.get(0).owner);
  }

  @Test
  void getTodosOwnedByJamie() throws IOException {
    String owner = "Jamie";
    when(ctx.queryParamMap()).thenReturn(Map.of("owner", List.of(owner)));
    when(ctx.queryParam("owner")).thenReturn(owner);

    todoController.getTodos(ctx);

    verify(ctx).json(todoArrayListCaptor.capture());
    verify(ctx).status(HttpStatus.OK);

    List<Todo> returnedTodos = todoArrayListCaptor.getValue();
    assertEquals(2, returnedTodos.size());
    assertEquals("Jamie", returnedTodos.get(0).owner);
    assertEquals("Jamie", returnedTodos.get(1).owner);
  }

  @Test
  void getTodosWithOwnerThatIsNotInAnyTodo() throws IOException {
    String owner = "asldkfjalskdfj";
    when(ctx.queryParamMap()).thenReturn(Map.of("owner", List.of(owner)));
    when(ctx.queryParam("owner")).thenReturn(owner);

    todoController.getTodos(ctx);

    verify(ctx).json(todoArrayListCaptor.capture());
    verify(ctx).status(HttpStatus.OK);

    List<Todo> returnedTodos = todoArrayListCaptor.getValue();
    assertEquals(0, returnedTodos.size());
  }

  @Test
  void getTodosWithEmptyOwnerParameter() throws IOException {
    String owner = "";
    when(ctx.queryParamMap()).thenReturn(Map.of("owner", List.of(owner)));
    when(ctx.queryParam("owner")).thenReturn(owner);

    todoController.getTodos(ctx);

    verify(ctx).json(todoArrayListCaptor.capture());
    verify(ctx).status(HttpStatus.OK);

    List<Todo> returnedTodos = todoArrayListCaptor.getValue();
    assertEquals(database.getCollection("todos").countDocuments(), returnedTodos.size());
  }

  @Test
  void getTodosWithCategoryOfSchool() throws IOException {
    String category = "school";
    when(ctx.queryParamMap()).thenReturn(Map.of("category", List.of(category)));
    when(ctx.queryParam("category")).thenReturn(category);

    todoController.getTodos(ctx);

    verify(ctx).json(todoArrayListCaptor.capture());
    verify(ctx).status(HttpStatus.OK);

    List<Todo> returnedTodos = todoArrayListCaptor.getValue();
    assertEquals(2, returnedTodos.size());
    assertEquals("school", returnedTodos.get(0).category);
    assertEquals("school", returnedTodos.get(1).category);
  }

  @Test
  void getTodosWithCategoryThatIsNotInAnyTodo() throws IOException {
    String category = "asldkfjalskdfj";
    when(ctx.queryParamMap()).thenReturn(Map.of("category", List.of(category)));
    when(ctx.queryParam("category")).thenReturn(category);

    todoController.getTodos(ctx);

    verify(ctx).json(todoArrayListCaptor.capture());
    verify(ctx).status(HttpStatus.OK);

    List<Todo> returnedTodos = todoArrayListCaptor.getValue();
    assertEquals(0, returnedTodos.size());
  }

  @Test
  void getTodosWithEmptyCategoryParameter() throws IOException {
    String category = "";
    when(ctx.queryParamMap()).thenReturn(Map.of("category", List.of(category)));
    when(ctx.queryParam("category")).thenReturn(category);

    todoController.getTodos(ctx);

    verify(ctx).json(todoArrayListCaptor.capture());
    verify(ctx).status(HttpStatus.OK);

    List<Todo> returnedTodos = todoArrayListCaptor.getValue();
    assertEquals(database.getCollection("todos").countDocuments(), returnedTodos.size());
  }

  @Test
  void getSortedTodosWithSortByOwner() throws IOException {
    String sortBy = "owner";
    when(ctx.queryParamMap()).thenReturn(Map.of("sortby", List.of(sortBy)));
    when(ctx.queryParam("sortby")).thenReturn(sortBy);

    todoController.getTodos(ctx);

    verify(ctx).json(todoArrayListCaptor.capture());
    verify(ctx).status(HttpStatus.OK);

    List<Todo> returnedTodos = todoArrayListCaptor.getValue();
    assertEquals(database.getCollection("todos").countDocuments(), returnedTodos.size());
    assertEquals("Chris", returnedTodos.get(0).owner);
    assertEquals("Guadalupe", returnedTodos.get(1).owner);
    assertEquals("Jamie", returnedTodos.get(2).owner);
    assertEquals("Jamie", returnedTodos.get(3).owner);
  }

  @Test
  void getSortedTodosWithSortByStatus() throws IOException {
    String sortBy = "status";
    when(ctx.queryParamMap()).thenReturn(Map.of("sortby", List.of(sortBy)));
    when(ctx.queryParam("sortby")).thenReturn(sortBy);

    todoController.getTodos(ctx);

    verify(ctx).json(todoArrayListCaptor.capture());
    verify(ctx).status(HttpStatus.OK);

    List<Todo> returnedTodos = todoArrayListCaptor.getValue();
    assertEquals(database.getCollection("todos").countDocuments(), returnedTodos.size());
    assertEquals(false, returnedTodos.get(0).status);
    assertEquals(true, returnedTodos.get(1).status);
    assertEquals(true, returnedTodos.get(2).status);
    assertEquals(true, returnedTodos.get(3).status);
  }

  @Test
  void getSortedTodosWithSortByBody() throws IOException {
    String sortBy = "body";
    when(ctx.queryParamMap()).thenReturn(Map.of("sortby", List.of(sortBy)));
    when(ctx.queryParam("sortby")).thenReturn(sortBy);

    todoController.getTodos(ctx);

    verify(ctx).json(todoArrayListCaptor.capture());
    verify(ctx).status(HttpStatus.OK);

    List<Todo> returnedTodos = todoArrayListCaptor.getValue();
    assertEquals(database.getCollection("todos").countDocuments(), returnedTodos.size());
    assertEquals("Eat more (7) vegetables.", returnedTodos.get(0).body);
    assertEquals("Finish Lab 2.", returnedTodos.get(1).body);
    assertEquals("Go to the gym.", returnedTodos.get(2).body);
    assertEquals("UMM is great!", returnedTodos.get(3).body);
  }

  @Test
  void getSortedTodosWithSortByCategory() throws IOException {
    String sortBy = "category";
    when(ctx.queryParamMap()).thenReturn(Map.of("sortby", List.of(sortBy)));
    when(ctx.queryParam("sortby")).thenReturn(sortBy);

    todoController.getTodos(ctx);

    verify(ctx).json(todoArrayListCaptor.capture());
    verify(ctx).status(HttpStatus.OK);

    List<Todo> returnedTodos = todoArrayListCaptor.getValue();
    assertEquals(database.getCollection("todos").countDocuments(), returnedTodos.size());
    assertEquals("home", returnedTodos.get(0).category);
    assertEquals("leisure", returnedTodos.get(1).category);
    assertEquals("school", returnedTodos.get(2).category);
    assertEquals("school", returnedTodos.get(3).category);
  }

  @Test
  void getSortedTodosWithInvalidSortByValue() throws IOException {
    String sortBy = "asldkfjalskdfj";
    when(ctx.queryParamMap()).thenReturn(Map.of("sortby", List.of(sortBy)));
    when(ctx.queryParam("sortby")).thenReturn(sortBy);

    BadRequestResponse exception = assertThrows(
      BadRequestResponse.class,
      () -> todoController.getTodos(ctx));

    assertEquals("Sort by parameter must be 'owner', 'status', 'body', or 'category'.", exception.getMessage());
  }

  @Test
  void getSortedTodosWithEmptySortByValue() throws IOException {
    String sortBy = "";
    when(ctx.queryParamMap()).thenReturn(Map.of("sortby", List.of(sortBy)));
    when(ctx.queryParam("sortby")).thenReturn(sortBy);

    BadRequestResponse exception = assertThrows(
      BadRequestResponse.class,
      () -> todoController.getTodos(ctx));

    assertEquals("Sort by parameter must be 'owner', 'status', 'body', or 'category'.", exception.getMessage());
  }
}
