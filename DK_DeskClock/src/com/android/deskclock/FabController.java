package com.android.deskclock;

import android.support.annotation.NonNull;
import android.view.View;
/*bv zhangjiachu add for alarm style 20200104 start*/
import android.widget.Button;
import android.widget.ImageButton;
import android.support.design.widget.TabLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
/*bv zhangjiachu add for alarm style 20200104 end*/
import android.widget.ImageView;

/**
 * Implementers of this interface are able to {@link #onUpdateFab configure the fab} and associated
 * {@link #onUpdateFabButtons left/right buttons} including setting them {@link View#INVISIBLE} if
 * they are unnecessary. Implementers also attach click handler logic to the
 * {@link #onFabClick fab}, {@link #onLeftButtonClick left button} and
 * {@link #onRightButtonClick right button}.
 */
public interface FabController {

    /**
     * Configures the display of the fab component to match the current state of this controller.
     *
     * @param fab the fab component to be configured based on current state
     */
    void onUpdateFab(@NonNull ImageView fab);

    void onUpdateTabs(@NonNull TabLayout tabs, @NonNull LinearLayout delete_alarm_tab, @NonNull TextView deskTitle);

    void onUpdateDelLayout(@NonNull LinearLayout delete_buttom_layout);

    void onUpdateDelClick(@NonNull TextView delete_buttom);

    void onUpdateCancel(@NonNull TextView cancel_delete);

    void onUpdateCancelClick(@NonNull TextView cancel_delete);

    void onUpdateSeleteAll(@NonNull TextView select_all);

    void onUpdateSeleteAllClick(@NonNull TextView select_all);

    void onUpdateSeleteNum(@NonNull TextView select_num);
    /*bv zhangjiachu add for alarm style 20200104 end*/
    /**
     * Called before onUpdateFab when the fab should be animated.
     *
     * @param fab the fab component to be configured based on current state
     */
    void onMorphFab(@NonNull ImageView fab);

    /**
     * Configures the display of the buttons to the left and right of the fab to match the current
     * state of this controller.
     *
     * @param left button to the left of the fab to configure based on current state
     * @param right button to the right of the fab to configure based on current state
     */
    /*bv zhangjiachu modify for alarm style 20200103 start*/
    void onUpdateFabButtons(@NonNull Button left, @NonNull Button right);
    void onUpdateFabButtons(@NonNull ImageButton left, @NonNull ImageButton right);
    /*bv zhangjiachu modify for alarm style 20200103 end*/

    /**
     * Handles a click on the fab.
     *
     * @param fab the fab component on which the click occurred
     */
    void onFabClick(@NonNull ImageView fab);

    /*bv zhangjiachu modify for alarm style 20200103 start*/
    /**
     * Handles a click on the button to the left of the fab component.
     *
     * @param left the button to the left of the fab component
     */
    void onLeftButtonClick(@NonNull Button left);
    void onLeftButtonClick(@NonNull ImageButton left);

    /**
     * Handles a click on the button to the right of the fab component.
     *
     * @param right the button to the right of the fab component
     */
    void onRightButtonClick(@NonNull Button right);
    void onRightButtonClick(@NonNull ImageButton right);
    /*bv zhangjiachu modify for alarm style 20200103 end*/
}