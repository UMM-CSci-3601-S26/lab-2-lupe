package umm3601.user;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.regex;

// import java.nio.charset.StandardCharsets;
// import java.security.MessageDigest;
// import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
// import java.util.Map;
// import java.util.Objects;
import java.util.regex.Pattern;

import org.bson.Document;
import org.bson.UuidRepresentation;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.mongojack.JacksonMongoCollection;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Sorts;
// import com.mongodb.client.result.DeleteResult;

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
  static final String CONTENT_KEY = "contains";
  static final String SORT_BY_KEY = "sortby";

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
    Bson filter = constructFilter(ctx);
    Bson sortingOrder = constructSortingOrder(ctx);
    int limit = 0;

    if (ctx.queryParamMap().containsKey(LIMIT_KEY)) {
        try {
            limit = Integer.parseInt(ctx.queryParam(LIMIT_KEY));
            if (limit < 0) {
                throw new BadRequestResponse("Limit must be a non-negative integer");
            }
        } catch (NumberFormatException e) {
            throw new BadRequestResponse("Limit must be a non-negative integer");
        }
    }

    List<Todo> matchingTodosList = todoCollection.find(filter)
      .sort(sortingOrder)
      .limit(limit)
      .into(new ArrayList<>());

    ctx.json(matchingTodosList);
    ctx.status(HttpStatus.OK);
  }

  private Bson constructFilter(Context ctx) {
    List<Bson> filters = new ArrayList<>();
    String tempString = "Status query parameter must be 'complete' or 'incomplete'.";

    if (ctx.queryParamMap().containsKey(STATUS_KEY)) {
      String status = ctx.queryParamAsClass(STATUS_KEY, String.class)
        .check(it -> it.equals("complete") || it.equals("incomplete"), tempString)
        .get();
      filters.add(eq(STATUS_KEY, status.equals("complete")));
    }

    if (ctx.queryParamMap().containsKey(CONTENT_KEY)) {
      String content = ctx.queryParam(CONTENT_KEY);
      filters.add(regex(BODY_KEY, Pattern.compile(content, Pattern.CASE_INSENSITIVE)));
    }

    if (ctx.queryParamMap().containsKey(OWNER_KEY)) {
      String owner = ctx.queryParam(OWNER_KEY);
      filters.add(regex(OWNER_KEY, Pattern.compile(owner, Pattern.CASE_INSENSITIVE)));
    }

    if (ctx.queryParamMap().containsKey(CATEGORY_KEY)) {
      String category = ctx.queryParam(CATEGORY_KEY);
      filters.add(regex(CATEGORY_KEY, Pattern.compile(category, Pattern.CASE_INSENSITIVE)));
    }

    Bson combinedFilter = filters.isEmpty() ? new Document() : and(filters);

    return combinedFilter;
  }

  private Bson constructSortingOrder(Context ctx) {
    if (ctx.queryParamMap().containsKey(SORT_BY_KEY)) {
      String sortBy = ctx.queryParam(SORT_BY_KEY);
      if (sortBy.equals(OWNER_KEY)||sortBy.equals(STATUS_KEY)||sortBy.equals(BODY_KEY)||sortBy.equals(CATEGORY_KEY)) {
        return Sorts.ascending(sortBy);
      } else {
        throw new BadRequestResponse("Sort by parameter must be 'owner', 'status', 'body', or 'category'.");
      }
    }
    return new Document();
  }

  @Override
  public void addRoutes(Javalin server) {
    server.get(API_TODOS, this::getTodos);
    server.get(API_TODOS + "/{id}", this::getTodo);
  }
}
