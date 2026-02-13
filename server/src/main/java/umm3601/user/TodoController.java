package umm3601.user;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.regex;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import org.bson.Document;
import org.bson.UuidRepresentation;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.mongojack.JacksonMongoCollection;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.result.DeleteResult;

import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.NotFoundResponse;
import umm3601.Controller;

public class TodoController implements Controller {
  private static final String API_TODOS = "/api/todos";
  static final String OWNER_KEY = "owner";
  static final String STATUS_KEY = "status";
  static final String BODY_KEY = "body";
  static final String CATEGORY_KEY = "category";
  static final String LIMIT_KEY = "limit";


  private final JacksonMongoCollection<Todo> todoCollection;

  public TodoController(MongoDatabase database) {
    todoCollection = JacksonMongoCollection.builder().build(
      database,
      "todos",
      Todo.class,
      UuidRepresentation.STANDARD);
    }

  public void getTodo(Context ctx) {
    String id = ctx.pathParam("id");
    Todo todo;
    try {
      todo = todoCollection.find(eq("_id", new ObjectId(id))).first();
    } catch (IllegalArgumentException e) {
      throw new BadRequestResponse("The requested todo id was not a legal Mongo Object ID.");
    }
    if (todo == null) {
      throw new NotFoundResponse("The requested todo was not found");
    } else {
      ctx.json(todo);
      ctx.status(HttpStatus.OK);
    }
  }

  public void getTodos(Context ctx) {
    Bson combinedFilter = constructFilter(ctx);
    Bson sortingOrder = constructSortingOrder(ctx);
    int limit = ctx.queryParamAsClass(LIMIT_KEY, Integer.class).getOrDefault(0);

    if (limit < 0) {
      throw new BadRequestResponse("The limit query parameter must be a non-negative integer.");
    }

     ArrayList<Todo> matchingTodos = todoCollection
    .find(combinedFilter).limit(limit)
    .sort(sortingOrder)
    .into(new ArrayList<>());

    ctx.json(matchingTodos);

    ctx.status(HttpStatus.OK);
  }

  private Bson constructFilter(Context ctx) {
    List<Bson> filters = new ArrayList<>();

    Bson combinedFilter = filters.isEmpty() ? new Document() : and(filters);

    return combinedFilter;
  }

  private Bson constructSortingOrder(Context ctx) {
    Bson sortingOrder = Sorts.ascending("owner");
    return sortingOrder;
  }

  @Override
  public void addRoutes(Javalin server) {
    server.get(API_TODOS, this::getTodos);
    server.get(API_TODOS + "/{id}", this::getTodo);
  }

}
