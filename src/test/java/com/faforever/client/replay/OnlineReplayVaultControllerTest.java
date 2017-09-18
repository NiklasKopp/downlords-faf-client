package com.faforever.client.replay;

import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.query.LogicalNodeController;
import com.faforever.client.query.SpecificationController;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.vault.search.SearchController;
import javafx.event.ActionEvent;
import javafx.scene.layout.Pane;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OnlineReplayVaultControllerTest extends AbstractPlainJavaFxTest {
  private static final int MAX_RESULTS = 100;

  private OnlineReplayVaultController instance;

  @Mock
  private ReplayService replayService;
  @Mock
  private UiService uiService;
  @Mock
  private ReplayDetailController replayDetailController;
  @Mock
  private SpecificationController specificationController;
  @Mock
  private LogicalNodeController logicalNodeController;
  @Mock
  private NotificationService notificationService;
  @Mock
  private I18n i18n;
  @Mock
  private SearchController searchController;

  @Captor
  private ArgumentCaptor<Consumer<String>> searchListenerCaptor;

  @Before
  public void setUp() throws Exception {
    when(uiService.loadFxml("theme/vault/replay/replay_card.fxml")).thenAnswer(invocation -> {
      ReplayCardController controller = mock(ReplayCardController.class);
      when(controller.getRoot()).thenReturn(new Pane());
      return controller;
    });

    instance = new OnlineReplayVaultController(replayService, uiService, notificationService, i18n);

    loadFxml("theme/vault/replay/online_replays.fxml", clazz -> {
      if (SearchController.class.isAssignableFrom(clazz)) {
        return searchController;
      }
      if (SpecificationController.class.isAssignableFrom(clazz)) {
        return specificationController;
      }
      if (LogicalNodeController.class.isAssignableFrom(clazz)) {
        return logicalNodeController;
      }
      return instance;
    });

    verify(searchController).setSearchListener(searchListenerCaptor.capture());
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), is(instance.replayVaultRoot));
    assertThat(instance.getRoot().getParent(), is(nullValue()));
  }

  @Test
  public void testOnDisplayPopulatesReplays() throws Exception {
    List<Replay> replays = Arrays.asList(new Replay(), new Replay());
    when(replayService.getNewestReplays(anyInt(), anyInt())).thenReturn(CompletableFuture.completedFuture(replays));
    when(replayService.getHighestRatedReplays(anyInt(), anyInt())).thenReturn(CompletableFuture.completedFuture(replays));
    when(replayService.getMostWatchedReplays(anyInt(), anyInt())).thenReturn(CompletableFuture.completedFuture(replays));

    instance.onDisplay();

    verify(replayService).getNewestReplays(anyInt(), eq(1));
    verify(replayService).getHighestRatedReplays(anyInt(), eq(1));
    verify(replayService).getMostWatchedReplays(anyInt(), eq(1));
    assertThat(instance.moreButton.isVisible(), is(false));
  }

  @Test
  public void testOnSearchButtonClicked() throws Exception {
    Consumer<String> searchListener = searchListenerCaptor.getValue();
    when(replayService.findByQuery("query", MAX_RESULTS, 1))
        .thenReturn(CompletableFuture.completedFuture(Arrays.asList(new Replay(), new Replay())));

    searchListener.accept("query");

    assertThat(instance.showroomGroup.isVisible(), is(false));
    assertThat(instance.searchResultGroup.isVisible(), is(true));
    verify(replayService).findByQuery("query", MAX_RESULTS, 1);
  }

  @Test
  public void testOnSearchButtonClickedHandlesException() throws Exception {
    Consumer<String> searchListener = searchListenerCaptor.getValue();
    CompletableFuture<List<Replay>> completableFuture = new CompletableFuture<>();
    completableFuture.completeExceptionally(new RuntimeException("JUnit test exception"));
    when(replayService.findByQuery("query", MAX_RESULTS, 1)).thenReturn(completableFuture);

    searchListener.accept("query");

    verify(notificationService).addNotification(any(ImmediateNotification.class));
  }

  @Test
  public void testMoreButton() throws Exception {
    Consumer<String> searchListener = searchListenerCaptor.getValue();
    List<Replay> list = new ArrayList<>();
    for (int i = 0; i != 100; i++) {
      list.add(new Replay());
    }
    CompletableFuture<List<Replay>> completableFuture = new CompletableFuture<>();
    completableFuture.complete(list);
    when(replayService.findByQuery(eq("query"), eq(MAX_RESULTS), anyInt())).thenReturn(completableFuture);

    searchListener.accept("query");

    instance.onLoadMoreButtonClicked(new ActionEvent());

    WaitForAsyncUtils.waitForFxEvents();
    verify(replayService).findByQuery("query", MAX_RESULTS, 1);
    verify(replayService).findByQuery("query", MAX_RESULTS, 2);
    assertThat(instance.moreButton.isVisible(), is(true));
  }

}
