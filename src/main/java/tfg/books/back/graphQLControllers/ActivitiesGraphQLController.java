package tfg.books.back.graphQLControllers;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import tfg.books.back.model.userActivity.UserActivity;
import tfg.books.back.model.userActivity.UserActivity.UserActivityType;
import tfg.books.back.services.UserActivityService;

import java.util.List;

@Controller
public class ActivitiesGraphQLController {

    private final UserActivityService userActivityService;

    public ActivitiesGraphQLController(UserActivityService userActivityService) {
        this.userActivityService = userActivityService;
    }

    @QueryMapping
    public List<UserActivity> getAllFollowedActivity(@Argument("timestamp") String timestamp) {
        return userActivityService.getAllFollowedActivity(timestamp);
    }

    @QueryMapping
    public List<UserActivity> getAllReviewsForBook(@Argument("bookId") String bookId) {
        return userActivityService.getAllReviewsForBook(bookId);
    }

    @MutationMapping
    public Boolean addActivity(@Argument("activityText") String activityText, @Argument("score") int score, @Argument(
            "bookId") String bookId, @Argument("userActivityType")UserActivityType userActivityType) {
        return userActivityService.addActivity(activityText, score, bookId, userActivityType);
    }

    @MutationMapping
    public Boolean deleteActivity(@Argument("activityId") String activityId) {
        return userActivityService.deleteActivity(activityId);
    }
}
