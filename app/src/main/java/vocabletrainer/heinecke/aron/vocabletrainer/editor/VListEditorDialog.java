package vocabletrainer.heinecke.aron.vocabletrainer.editor;

import androidx.appcompat.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import java.util.concurrent.Callable;

import vocabletrainer.heinecke.aron.vocabletrainer.R;
import vocabletrainer.heinecke.aron.vocabletrainer.lib.Storage.VList;

/**
 * Dialog for list metadata editing
 */
public class VListEditorDialog extends DialogFragment {
    public static final String TAG = "VListEditorDialog";
    private final static String PARAM_NEW = "is_new";
    private final static String KEY_COL_A = "colA";
    private final static String KEY_COL_B = "colB";
    private final static String KEY_Name = "name";
    private Callable<Void> okAction;
    private Callable<Void> cancelAction;
    private boolean newList;
    private VList list;
    private EditText iName;
    private EditText iColA;
    private EditText iColB;

    /**
     * Creates a new instance<br>
     *     see {@link #getListProvider()} for VList provider requirements
     * @param isNew true if a new list is created
     * @return VListEditorDialog
     */
    public static VListEditorDialog newInstance(final boolean isNew){
        VListEditorDialog dialog = new VListEditorDialog();

        Bundle bundle = new Bundle();
        bundle.putBoolean(PARAM_NEW, isNew);
        dialog.setArguments(bundle);

        return dialog;
    }

    /**
     * Set ok action to run afterwards
     * @param okAction
     */
    public void setOkAction(Callable<Void> okAction) {
        this.okAction = okAction;
    }

    /**
     * Set cancel action to run afterwards
     * @param cancelAction
     */
    public void setCancelAction(Callable<Void> cancelAction) {
        this.cancelAction = cancelAction;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // we have to override this one, because we're inflating + using the builder, apparently
        setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomDialog);
        Log.d(TAG,"onCreate");
        if(savedInstanceState != null) {
            newList = getArguments().getBoolean(PARAM_NEW);
        } else if(getArguments() != null){
            newList = getArguments().getBoolean(PARAM_NEW);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG,"saving..");
        outState.putBoolean(PARAM_NEW,newList);
        outState.putString(KEY_COL_A,iColA.getText().toString());
        outState.putString(KEY_COL_B,iColB.getText().toString());
        outState.putString(KEY_Name,iName.getText().toString());
    }

    /**
     * Get ListEditorDataProvider<br>
     *     Allows provider to be a targetFragment, parentFragment or the activity
     * @return
     */
    private ListEditorDataProvider getListProvider() {
        if(getTargetFragment() instanceof  ListEditorDataProvider){
            return (ListEditorDataProvider) getTargetFragment();
        }else if(getParentFragment() instanceof ListEditorDataProvider){
            return (ListEditorDataProvider) getParentFragment();
        } else if (getActivity() instanceof ListEditorDataProvider) {
            return (ListEditorDataProvider) getActivity();
        } else {
            throw new IllegalStateException("No VList provider found!");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        list = getListProvider().getList();
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(requireActivity(),R.style.CustomDialog);
        final View view = View.inflate(getActivity(), R.layout.dialog_list, null);
        // and we have to use this one, together with onCreate, because of the inflation here
        // to override dialog buttons + inflated view
        view.getContext().getTheme().applyStyle(R.style.CustomDialog, true);
        alertDialog.setTitle(newList ? R.string.Editor_Diag_table_Title_New : R.string.Editor_Diag_table_Title_Edit );
        alertDialog.setView(view);
        iName = view.findViewById(R.id.tListName);
        iColA = view.findViewById(R.id.tListColumnA);
        iColB = view.findViewById(R.id.tListColumnB);

        iName.setText(list.getName());
        iName.setSingleLine();
        iColA.setText(list.getNameA());
        iColA.setSingleLine();
        iColB.setSingleLine();
        iColB.setText(list.getNameB());
        iColB.setOnEditorActionListener((textView, actionID, keyEvent) -> {
            if(actionID == EditorInfo.IME_ACTION_DONE){
                okAction();
                dismiss();
                return true;
            }
            return false;
        });
        if (newList) {
            iName.setSelectAllOnFocus(true);
            iColA.setSelectAllOnFocus(true);
            iColB.setSelectAllOnFocus(true);
        }

        if(savedInstanceState != null){
            iName.setText(savedInstanceState.getString(KEY_Name));
            iColA.setText(savedInstanceState.getString(KEY_COL_A));
            iColB.setText(savedInstanceState.getString(KEY_COL_B));
        }

        alertDialog.setPositiveButton(R.string.GEN_OK, (dialog, whichButton) -> {
            okAction();
        });
        alertDialog.setNegativeButton(R.string.Editor_Diag_table_btn_Canel, (dialog, which) -> callCancelAction());
        return alertDialog.create();
    }

    /**
     * Action to perform on ok, doesn't close the dialog by itself
     */
    private void okAction() {
        if (iColA.getText().length() == 0 || iColB.length() == 0 || iName.getText().length() == 0) {
            Log.d(TAG, "empty insert");
        }

        list.setNameA(iColA.getText().toString());
        list.setNameB(iColB.getText().toString());
        list.setName(iName.getText().toString());
        if(okAction != null){
            try {
                okAction.call();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        callCancelAction();
    }

    /**
     * Calls cancel action
     */
    private void callCancelAction(){
        if(cancelAction != null){
            try {
                cancelAction.call();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Interface for dialog caller
     */

    public interface ListEditorDataProvider {
        @NonNull VList getList();
    }
}
