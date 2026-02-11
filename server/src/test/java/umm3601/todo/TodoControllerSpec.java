package umm3601.todo;

import static com.mongodb.client.model.Filters.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.eclipse.jetty.util.IO;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
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
import io.javalin.validation.BodyValidator;
import io.javalin.validation.Validation;
import io.javalin.validation.ValidationError;
import io.javalin.validation.ValidationException;
import io.javalin.validation.Validator;

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
        .append("owner", "Pat")
        .append("status", true)
        .append("body", "Eat more vegetables.")
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
    verify(mockServer, Mockito.atLeast(1)).get(any(), any());//come back and adjust
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

}


