package tfg.books.back.integrationTest.bookActivityBook;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import tfg.books.back.BackApplication;
import tfg.books.back.FirebaseTestConfig;
import tfg.books.back.firebase.AuthenticatedUserIdProvider;
import tfg.books.back.graphQLControllers.BookGraphQLController;
import tfg.books.back.integrationTest.ConfigurationTest;
import tfg.books.back.model.books.ExtraInfoForBook;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest(classes = {BackApplication.class, FirebaseTestConfig.class, ConfigurationTest.class})
public class GetExtraInfoForBookTest {
    @Autowired
    private ConfigurationTest configurationTest;

    @MockitoSpyBean
    private BookGraphQLController bookAPIService;

    @MockitoBean
    private AuthenticatedUserIdProvider authenticatedUserIdProvider;

    @BeforeEach
    public void setup() throws ExecutionException, InterruptedException {
        configurationTest.preloadData();
        when(authenticatedUserIdProvider.getUserId()).thenReturn("userId1");

    }

    @Test
    public void PU28_testGetAllFollowedActivity() {
        ExtraInfoForBook bookInfo = bookAPIService.getExtraInfoForBook("bookId");

        assertThat(bookInfo).isNotNull();
        assertThat(bookInfo.userScore()).isEqualTo(5);
        assertThat(bookInfo.meanScore()).isEqualTo(5);
        assertThat(bookInfo.numberOfReviews()).isEqualTo(1);
        assertThat(bookInfo.userProfilePictures().size()).isEqualTo(1);
        assertThat(bookInfo.numberOfRatings()).isEqualTo(6);
    }

    @Test
    public void PU29_testGetAllFollowedActivity() {
        ExtraInfoForBook bookInfo = bookAPIService.getExtraInfoForBook("book");

        assertThat(bookInfo).isNotNull();
        assertThat(bookInfo.userScore()).isEqualTo(0);
        assertThat(bookInfo.meanScore()).isEqualTo(0);
        assertThat(bookInfo.numberOfReviews()).isEqualTo(0);
        assertThat(bookInfo.userProfilePictures().size()).isEqualTo(0);
        assertThat(bookInfo.numberOfRatings()).isEqualTo(0);
    }
}
