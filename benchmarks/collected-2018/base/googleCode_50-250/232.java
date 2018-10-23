// https://searchcode.com/api/result/12997307/

/*
 * Copyright 2010 The Rabbit Eclipse Plug-in Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package rabbit.ui.internal.pages;

import static rabbit.ui.internal.viewers.Viewers.refresh;
import static rabbit.ui.internal.viewers.Viewers.resetInput;

import rabbit.ui.IPage;
import rabbit.ui.internal.util.ICategory;
import rabbit.ui.internal.util.ICategoryProvider;
import rabbit.ui.internal.util.IVisualProvider;
import rabbit.ui.internal.util.TreePathValueProvider;
import rabbit.ui.internal.viewers.IValueProvider;

import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.dialogs.FilteredTree;

import java.util.List;
import java.util.Observable;
import java.util.Observer;

/**
 * Internal abstract page to reduce code duplication.
 */
public abstract class AbsPage implements IPage, Observer {

  @Override
  public void onRestoreState(IMemento memento) {
    String id = getClass().getSimpleName();
    TreeColumn[] columns = getFilteredTree().getViewer().getTree().getColumns();
    
    StateHelper helper = StateHelper.of(memento, id);
    helper.restoreColumnWidths(columns);
    
    Category visual = helper.retrieveSavedVisualCategory();
    if (visual != null) {
      setVisualCategory(visual);
    }
    
    List<Category> selected = helper.retrieveSavedCategories();
    if (selected != null) {
      setSelectedCategories(selected);
    }
  }

  @Override
  public void onSaveState(IMemento memento) {
    String id = getClass().getSimpleName();
    TreeColumn[] columns = getFilteredTree().getViewer().getTree().getColumns();
    StateHelper
        .of(memento, id)
        .saveColumnWidths(columns)
        .saveVisualCategory(getVisualCategory())
        .saveCategories(getSelectedCategories());
  }

  @Override
  public void update(Observable o, Object arg) {
    if (o instanceof IVisualProvider) {
      updateMaxValue();
      refresh(getFilteredTree().getViewer());

    } else if (o instanceof ICategoryProvider) {
      resetInput(getFilteredTree().getViewer());

    } else if (o instanceof IContentProvider) {
      updateMaxValue();
    }
  }
  
  protected abstract FilteredTree getFilteredTree();
  
  /**
   * @return the current selected categories.
   * @see ICategoryProvider#getSelected()
   */
  protected abstract Category[] getSelectedCategories();
  
  /**
   * @return the current visual category.
   * @see IVisualProvider#getVisualCategory()
   */
  protected abstract Category getVisualCategory();
  
  /**
   * @param categories the categories to set.
   * @see ICategoryProvider#setSelected(ICategory...)
   */
  protected abstract void setSelectedCategories(List<Category> categories);
  
  /**
   * @param category the category to set.
   * @see TreePathValueProvider#setVisualCategory(ICategory)
   */
  protected abstract void setVisualCategory(Category category);
  
  /**
   * Updates {@link IValueProvider#getMaxValue()}.
   */
  protected abstract void updateMaxValue();
}

