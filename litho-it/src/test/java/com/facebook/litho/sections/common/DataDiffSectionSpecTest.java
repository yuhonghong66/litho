/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.sections.common;

import static com.facebook.litho.testing.sections.TestTarget.DELETE;
import static com.facebook.litho.testing.sections.TestTarget.INSERT;
import static com.facebook.litho.testing.sections.TestTarget.MOVE;
import static junit.framework.Assert.assertEquals;
import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.EventHandler;
import com.facebook.litho.HasEventDispatcher;
import com.facebook.litho.sections.Section;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.SectionTree;
import com.facebook.litho.testing.sections.TestGroupSection;
import com.facebook.litho.testing.sections.TestTarget;
import com.facebook.litho.testing.sections.TestTarget.Operation;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

/** Tests {@link DataDiffSectionSpec} */
@RunWith(ComponentsTestRunner.class)
public class DataDiffSectionSpecTest {

  private SectionContext mSectionContext;
  private SectionTree mSectionTree;
  private TestTarget mTestTarget;

  @Mock public EventHandler<OnCheckIsSameItemEvent> mIsSameItemEventEventHandler;
  @Mock public HasEventDispatcher mHasEventDispatcher;

  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);
    mSectionContext = new SectionContext(RuntimeEnvironment.application);
    mTestTarget = new TestTarget();
    mSectionTree = SectionTree.create(mSectionContext, mTestTarget).build();
  }

  @Test
  public void testSetRoot() {
    mSectionTree.setRoot(TestGroupSection.create(mSectionContext).data(generateData(100)).build());
    final List<Operation> executedOperations = mTestTarget.getOperations();

    assertThat(executedOperations.size()).isEqualTo(1);
    assertRangeOperation(executedOperations.get(0), TestTarget.INSERT_RANGE, 0, 100);
  }

  @Test
  public void testAppendData() {
    mSectionTree.setRoot(TestGroupSection.create(mSectionContext).data(generateData(100)).build());
    List<Operation> executedOperations = mTestTarget.getOperations();

    assertThat(executedOperations.size()).isEqualTo(1);
    assertRangeOperation(executedOperations.get(0), TestTarget.INSERT_RANGE, 0, 100);

    mTestTarget.clear();

    mSectionTree.setRoot(TestGroupSection.create(mSectionContext).data(generateData(200)).build());
    executedOperations = mTestTarget.getOperations();

    assertThat(executedOperations.size()).isEqualTo(1);
    assertRangeOperation(executedOperations.get(0), TestTarget.INSERT_RANGE, 100, 100);
  }

  @Test
  public void testInsertData() {
    mSectionTree.setRoot(TestGroupSection.create(mSectionContext).data(generateData(100)).build());
    List<Operation> executedOperations = mTestTarget.getOperations();

    assertThat(executedOperations.size()).isEqualTo(1);
    assertRangeOperation(executedOperations.get(0), TestTarget.INSERT_RANGE, 0, 100);

    mTestTarget.clear();

    final List<String> data = generateData(100);
    data.add(6,"new item");
    data.add(9,"new item");
    data.add(12,"new item");

    mSectionTree.setRoot(TestGroupSection.create(mSectionContext).data(data).build());
    executedOperations = mTestTarget.getOperations();

    assertThat(executedOperations.size()).isEqualTo(3);
    assertThat(executedOperations.get(0).mOp).isEqualTo(INSERT);
    assertThat(executedOperations.get(0).mIndex).isEqualTo(10);

    assertThat(executedOperations.get(1).mOp).isEqualTo(INSERT);
    assertThat(executedOperations.get(1).mIndex).isEqualTo(8);

    assertThat(executedOperations.get(2).mOp).isEqualTo(INSERT);
    assertThat(executedOperations.get(2).mIndex).isEqualTo(6);
  }

  @Test
  public void testMoveData() {
    mSectionTree.setRoot(TestGroupSection.create(mSectionContext).data(generateData(3)).build());
    List<Operation> executedOperations = mTestTarget.getOperations();

    assertThat(executedOperations.size()).isEqualTo(1);
    assertRangeOperation(executedOperations.get(0), TestTarget.INSERT_RANGE, 0, 3);

    mTestTarget.clear();

    List<String> data = new ArrayList<>();
    for (int i = 2; i >= 0; i--) {
      data.add(Integer.toString(i));
    }

    mSectionTree.setRoot(TestGroupSection.create(mSectionContext).data(data).build());
    executedOperations = mTestTarget.getOperations();

    assertThat(executedOperations.size()).isEqualTo(2);

    assertThat(executedOperations.get(0).mOp).isEqualTo(MOVE);
    assertThat(executedOperations.get(0).mIndex).isEqualTo(1);
    assertThat(executedOperations.get(0).mToIndex).isEqualTo(0);

    assertThat(executedOperations.get(1).mOp).isEqualTo(MOVE);
    assertThat(executedOperations.get(1).mIndex).isEqualTo(2);
    assertThat(executedOperations.get(1).mToIndex).isEqualTo(0);
  }

  @Test
  public void testRemoveRangeData() {
    mSectionTree.setRoot(TestGroupSection.create(mSectionContext).data(generateData(100)).build());
    List<Operation> executedOperations = mTestTarget.getOperations();

    assertThat(executedOperations.size()).isEqualTo(1);
    assertRangeOperation(executedOperations.get(0), TestTarget.INSERT_RANGE, 0, 100);

    mTestTarget.clear();

    final List<String> data = new ArrayList<>();
    for (int i = 0; i < 50; i++) {
      data.add(""+i);
    }
    for (int i = 90; i < 100; i++) {
      data.add(""+i);
    }
    // data = [0...49, 90...99]

    mSectionTree.setRoot(TestGroupSection.create(mSectionContext).data(data).build());
    executedOperations = mTestTarget.getOperations();

    assertThat(1).isEqualTo(executedOperations.size());
    assertRangeOperation(executedOperations.get(0), TestTarget.DELETE_RANGE, 50, 40);
  }

  @Test
  public void testRemoveData() throws Exception {
    mSectionTree.setRoot(TestGroupSection.create(mSectionContext).data(generateData(100)).build());
    List<Operation> executedOperations = mTestTarget.getOperations();

    assertThat(executedOperations.size()).isEqualTo(1);
    assertRangeOperation(executedOperations.get(0), TestTarget.INSERT_RANGE, 0, 100);

    mTestTarget.clear();

    final List<String> data = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      data.add(""+i);
    }
    data.remove(9);
    data.remove(91);

    mSectionTree.setRoot(TestGroupSection.create(mSectionContext).data(data).build());
    executedOperations = mTestTarget.getOperations();

    assertThat(2).isEqualTo(executedOperations.size());
    assertThat(executedOperations.get(0).mOp).isEqualTo(DELETE);
    assertThat(executedOperations.get(0).mIndex).isEqualTo(92);
    assertThat(executedOperations.get(1).mOp).isEqualTo(DELETE);
    assertThat(executedOperations.get(1).mIndex).isEqualTo(9);
  }

  @Test
  public void testUpdateData() {
    mSectionTree.setRoot(TestGroupSection.create(mSectionContext).data(generateData(100)).build());
    List<Operation> executedOperations = mTestTarget.getOperations();

    assertThat(executedOperations.size()).isEqualTo(1);
    assertRangeOperation(executedOperations.get(0), TestTarget.INSERT_RANGE, 0, 100);

    mTestTarget.clear();

    List<String> data = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      data.add("different "+i);
    }

    mSectionTree.setRoot(TestGroupSection
        .create(mSectionContext)
        .isSameItemComparator(new Comparator() {
          @Override
          public int compare(Object lhs, Object rhs) {
            return 0;
          }
        })
        .isSameContentComparator(new Comparator() {
          @Override
          public int compare(Object lhs, Object rhs) {
            return -1;
          }
        })
        .data(data)
        .build());
    executedOperations = mTestTarget.getOperations();

    assertThat(executedOperations.size()).isEqualTo(1);
    assertRangeOperation(executedOperations.get(0), TestTarget.UPDATE_RANGE, 0, 100);
  }

  @Test
  public void testShuffledDataWithUpdates() {
    final List<String> oldData = generateData(40);
    Collections.shuffle(oldData);

    mSectionTree.setRoot(TestGroupSection.create(mSectionContext).data(oldData).build());
    List<Operation> executedOperations = mTestTarget.getOperations();

    assertThat(executedOperations.size()).isEqualTo(1);
    assertRangeOperation(executedOperations.get(0), TestTarget.INSERT_RANGE, 0, 40);

    mTestTarget.clear();

    final List<String> newData = generateData(20);
    Collections.shuffle(newData);

    mSectionTree.setRoot(
        TestGroupSection.create(mSectionContext)
            .isSameItemComparator(
                new Comparator() {
                  @Override
                  public int compare(Object lhs, Object rhs) {
                    String left = (String) lhs;
                    String right = (String) rhs;
                    return left.compareTo(right);
                  }
                })
            .isSameContentComparator(
                new Comparator() {
                  @Override
                  public int compare(Object lhs, Object rhs) {
                    return -1;
                  }
                })
            .data(newData)
            .build());

    executedOperations = mTestTarget.getOperations();
    assertBulkOperations(executedOperations,0, 20,20);
  }


  @Test
  public void testShuffledData() {
    final List<String> oldData = generateData(40);
    Collections.shuffle(oldData);

    mSectionTree.setRoot(TestGroupSection.create(mSectionContext).data(oldData).build());
    List<Operation> executedOperations = mTestTarget.getOperations();

    assertThat(executedOperations.size()).isEqualTo(1);
    assertRangeOperation(executedOperations.get(0), TestTarget.INSERT_RANGE, 0, 40);

    mTestTarget.clear();

    final List<String> newData = generateData(20);
    Collections.shuffle(newData);

    mSectionTree.setRoot(
        TestGroupSection.create(mSectionContext)
            .isSameItemComparator(
                new Comparator() {
                  @Override
                  public int compare(Object lhs, Object rhs) {
                    String left = (String) lhs;
                    String right = (String) rhs;
                    return left.compareTo(right);
                  }
                })
            .data(newData)
            .build());

    executedOperations = mTestTarget.getOperations();

    assertBulkOperations(executedOperations, 0, 0, 20);
  }

  @Test
  public void testTrimmingHeadEqualInstancesOnly() {
    ArrayList<String> previousData = new ArrayList<>();
    ArrayList<String> nextData = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      String item = "" + i;
      previousData.add(item);
      nextData.add(item);
    }

    for (int i = 0; i < 10; i++) {
      previousData.add("" + i);
      nextData.add("" + i);
    }

    DataDiffSection.Builder builder =
        DataDiffSection.<String>create(mSectionContext).data(previousData).renderEventHandler(null);

    mSectionContext = SectionContext.withScope(mSectionContext, builder.build());

    final DataDiffSectionSpec.Callback<String> callback =
        DataDiffSectionSpec.Callback.acquire(
            mSectionContext,
            previousData,
            nextData,
            true /*trimHeadAndTail*/,
            true /*trimSameInstancesOnly*/);

    assertThat(callback.getTrimmedHeadItemsCount()).isEqualTo(10);
    assertThat(callback.getOldListSize()).isEqualTo(10);
    assertThat(callback.getNewListSize()).isEqualTo(10);
  }

  @Test
  public void testTrimmingTailEqualInstancesOnly() {
    ArrayList<String> previousData = new ArrayList<>();
    ArrayList<String> nextData = new ArrayList<>();

    for (int i = 0; i < 10; i++) {
      previousData.add("" + i);
      nextData.add("" + i);
    }

    for (int i = 0; i < 10; i++) {
      String item = "" + i;
      previousData.add(item);
      nextData.add(item);
    }

    DataDiffSection.Builder builder =
        DataDiffSection.<String>create(mSectionContext).data(previousData).renderEventHandler(null);

    mSectionContext = SectionContext.withScope(mSectionContext, builder.build());

    final DataDiffSectionSpec.Callback<String> callback =
        DataDiffSectionSpec.Callback.acquire(
            mSectionContext,
            previousData,
            nextData,
            true /*trimHeadAndTail*/,
            true /*trimSameInstancesOnly*/);

    assertThat(callback.getTrimmedHeadItemsCount()).isEqualTo(0);
    assertThat(callback.getOldListSize()).isEqualTo(10);
    assertThat(callback.getNewListSize()).isEqualTo(10);
  }

  @Test
  public void testTrimmingHeadAndTailEqualInstancesOnly() {
    ArrayList<String> previousData = new ArrayList<>();
    ArrayList<String> nextData = new ArrayList<>();

    for (int i = 0; i < 10; i++) {
      String item = "" + i;
      previousData.add(item);
      nextData.add(item);
    }

    for (int i = 0; i < 10; i++) {
      previousData.add("" + i);
      nextData.add("" + i);
    }

    for (int i = 0; i < 10; i++) {
      String item = "" + i;
      previousData.add(item);
      nextData.add(item);
    }

    DataDiffSection.Builder builder =
        DataDiffSection.<String>create(mSectionContext).data(previousData).renderEventHandler(null);

    mSectionContext = SectionContext.withScope(mSectionContext, builder.build());

    final DataDiffSectionSpec.Callback<String> callback =
        DataDiffSectionSpec.Callback.acquire(
            mSectionContext,
            previousData,
            nextData,
            true /*trimHeadAndTail*/,
            true /*trimSameInstancesOnly*/);

    assertThat(callback.getTrimmedHeadItemsCount()).isEqualTo(10);
    assertThat(callback.getOldListSize()).isEqualTo(10);
    assertThat(callback.getNewListSize()).isEqualTo(10);
  }

  @Test
  public void testTrimmingWithComparisonHandlers() {
    ArrayList<String> previousData = new ArrayList<>();
    ArrayList<String> nextData = new ArrayList<>();

    for (int i = 0; i < 10; i++) {
      previousData.add(i, "*" + i);
      nextData.add(i, "*" + i);
    }

    for (int i = 10; i < 20; i++) {
      previousData.add(i, "#" + i);
      nextData.add(i, "#" + i);
    }

    Section dispatcher =
        TestGroupSection.create(mSectionContext)
            .data(nextData)
            .isSameItemComparator(
                new Comparator() {
                  @Override
                  public int compare(Object lhs, Object rhs) {
                    String prev = (String) lhs;
                    String next = (String) rhs;
                    return (prev.contains("*") && next.contains("*")) ? 0 : 1;
                  }
                })
            .build();
    mSectionContext = SectionContext.withSectionTree(mSectionContext, mSectionTree);
    mSectionContext = SectionContext.withScope(mSectionContext, dispatcher);
    dispatcher.setScopedContext(mSectionContext);

    EventHandler eh = TestGroupSection.onCheckIsSameItem(mSectionContext);

    EventHandler same =
        new EventHandler(mHasEventDispatcher, eh.id, new Object[] {mSectionContext});
    same.mHasEventDispatcher = dispatcher;

    DataDiffSection builder =
        DataDiffSection.<String>create(mSectionContext)
            .data(previousData)
            .onCheckIsSameItemEventHandler(same)
            .renderEventHandler(null)
            .build();

    mSectionContext = SectionContext.withSectionTree(mSectionContext, mSectionTree);
    mSectionContext = SectionContext.withScope(mSectionContext, builder);
    builder.setScopedContext(mSectionContext);

    final DataDiffSectionSpec.Callback<String> callback =
        DataDiffSectionSpec.Callback.acquire(
            mSectionContext,
            previousData,
            nextData,
            true /*trimHeadAndTail*/,
            false /*trimSameInstancesOnly*/);

    assertThat(callback.getTrimmedHeadItemsCount()).isEqualTo(10);
    assertThat(callback.getOldListSize()).isEqualTo(10);
    assertThat(callback.getNewListSize()).isEqualTo(10);
  }

  @Test
  public void testAppendDataTrimming() {
    mSectionTree.setRoot(
        TestGroupSection.create(mSectionContext)
            .data(generateData(100))
            .trimHeadAndTail(true)
            .trimSameInstancesOnly(true)
            .build());
    List<Operation> executedOperations = mTestTarget.getOperations();

    assertThat(executedOperations.size()).isEqualTo(1);
    assertRangeOperation(executedOperations.get(0), TestTarget.INSERT_RANGE, 0, 100);

    mTestTarget.clear();

    mSectionTree.setRoot(TestGroupSection.create(mSectionContext).data(generateData(200)).build());
    executedOperations = mTestTarget.getOperations();

    assertThat(executedOperations.size()).isEqualTo(1);
    assertRangeOperation(executedOperations.get(0), TestTarget.INSERT_RANGE, 100, 100);
  }

  @Test
  public void testLogTag() {
    ArrayList<String> data = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      data.add(String.valueOf(i));
    }
    DataDiffSection section =
        DataDiffSection.<String>create(mSectionContext).data(data).renderEventHandler(null).build();
    assertThat(section.getLogTag()).isEqualTo(section.getClass().getSimpleName());
  }

  private void assertRangeOperation(
      Operation operation,
      int opType,
      int startIndex,
      int rangeCount) {
    assertEquals("opreation type", operation.mOp, opType);
    assertEquals("operation starting index", operation.mIndex, startIndex);
    assertEquals("operation range count", operation.mRangeCount, rangeCount);
  }

  private void assertBulkOperations(
      List<Operation> operations,
      int expectedInserted,
      int expectedUpdated,
      int expectedRemoved) {

    int totalRemoved = 0;
    int totalUpdated = 0;
    int totalInserted = 0;

    for (int i = 0; i < operations.size(); i++) {
      Operation operation = operations.get(i);

      switch (operation.mOp) {
        case TestTarget.DELETE:
          totalRemoved++;
          break;
        case TestTarget.DELETE_RANGE:
          totalRemoved += operation.mRangeCount;
          break;
        case TestTarget.UPDATE:
          totalUpdated++;
          break;
        case TestTarget.UPDATE_RANGE:
          totalUpdated += operation.mRangeCount;
          break;
        case TestTarget.INSERT:
          totalInserted++;
          break;
        case TestTarget.INSERT_RANGE:
          totalInserted += operation.mRangeCount;
          break;
      }
    }

    assertThat(totalInserted).isEqualTo(expectedInserted);
    assertThat(totalUpdated).isEqualTo(expectedUpdated);
    assertThat(totalRemoved).isEqualTo(expectedRemoved);
  }

  private static List<String> generateData(int length) {
    final List<String> data = new ArrayList<>(length);
    for (int i = 0; i < length; i++) {
      data.add(Integer.toString(i));
    }
    return data;
  }
}
