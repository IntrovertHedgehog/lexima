package vocabletrainer.heinecke.aron.vocabletrainer.eximport;

import android.app.Activity;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.Callable;

import vocabletrainer.heinecke.aron.vocabletrainer.R;
import vocabletrainer.heinecke.aron.vocabletrainer.editor.ListActivity;
import vocabletrainer.heinecke.aron.vocabletrainer.editor.VListEditorDialog;
import vocabletrainer.heinecke.aron.vocabletrainer.fragment.BaseFragment;
import vocabletrainer.heinecke.aron.vocabletrainer.eximport.CSV.CSVCustomFormat;
import vocabletrainer.heinecke.aron.vocabletrainer.eximport.CSV.Import.ImportFetcher;
import vocabletrainer.heinecke.aron.vocabletrainer.eximport.CSV.Import.Importer;
import vocabletrainer.heinecke.aron.vocabletrainer.lib.Storage.VList;
import vocabletrainer.heinecke.aron.vocabletrainer.lib.StorageUtils;
import vocabletrainer.heinecke.aron.vocabletrainer.lib.Widget.CustomItemSelectedListener;
import vocabletrainer.heinecke.aron.vocabletrainer.lib.Widget.ViewCreation;

import static vocabletrainer.heinecke.aron.vocabletrainer.activity.MainActivity.PREFS_NAME;

/*
 * Import Activity
 * @author Aron Heinecke
 */
public class ImportFragment extends BaseFragment implements VListEditorDialog.ListEditorDataProvider {
    private static final String TAG_PROGRESS_IMPORT = "progressImport";
    private static final String TAG_PROGRESS_REPARSE = "progressReparse";
    private static final String P_KEY_I_IMP_MULTI = "import_sp_multi";
    private static final String P_KEY_I_IMP_SINGLE = "import_sp_single";
    private static final String P_KEY_I_IMP_RAW = "import_sp_raw";
    private static final String P_KEY_I_IMP_FORMAT = "import_sp_format";
    private static final int REQUEST_LIST_SELECT_CODE = 2;
    public static final String TAG = "ImportFragment";
    private static final String KEY_LIST_TARGET = "targetList";
    private static final String KEY_URI = "URI";
    private static final String KEY_PARSING_INVALIDATED = "parsing_invalidated";
    private static final String KEY_FILE_PATH = "filePath";
    VList targetList;
    ArrayAdapter<GenericSpinnerEntry<CSVCustomFormat>> spAdapterFormat;
    private RadioGroup rgSingle, rgRaw, rgMulti;
    private Button bReparse;
    private Spinner spFormat;
    private Button bSelectList;
    private EditText etList;
    private Uri selectedFile = null;
    private EditText etFile;
    private Button bImportOk;
    private ConstraintLayout singleLayout;
    private TextView tInfo;
    private ImportFetcher.MessageProvider mp;
    private ImportViewModel importViewModel;
    private ExImportActivity activity;
    private GenericSpinnerEntry<CSVCustomFormat> customFormatEntry;
    private Importer.IMPORT_LIST_MODE importListMode;

    private ProgressDialog progressDialogImport;
    private ProgressDialog progressDialogReparse;
    private VListEditorDialog listEditorDialog;
    private FormatViewModel formatViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"onCreate");
        // init viewmodel observers here, onCreateView can be re-called on tab change (memory free?)
        if(savedInstanceState != null){
            progressDialogImport = (ProgressDialog) getACActivity().getSupportFragmentManager().getFragment(savedInstanceState, TAG_PROGRESS_IMPORT);
            progressDialogReparse = (ProgressDialog) getACActivity().getSupportFragmentManager().getFragment(savedInstanceState, TAG_PROGRESS_REPARSE);
            listEditorDialog = (VListEditorDialog) getACActivity().getSupportFragmentManager().getFragment(savedInstanceState,VListEditorDialog.TAG);
            if(listEditorDialog != null) {
                Log.d(TAG, "re-init editor Dialog");
                setListEditorAction();
            }
        }

        importViewModel = ViewModelProviders.of(getActivity()).get(ImportViewModel.class);
        formatViewModel = ViewModelProviders.of(getActivity()).get(FormatViewModel.class);

        importViewModel.getLogHandle().observe(this, logData -> {
            if(logData != null && logData.log.length() > 0) {
                ImportLogDialog logDialog = ImportLogDialog.newInstance(logData);
                logDialog.show(getACActivity().getSupportFragmentManager(),ImportLogDialog.TAG);
                importViewModel.resetLog();
            }
        });

        // update parsing on custom format change, if in use
        formatViewModel.getCustomFormatLD().observe(this, obs -> {
            if(obs == null)
                return;
            customFormatEntry.updateObject(obs);
            if(getFormatSelected() == obs) {
                Log.d(TAG,"new format is selected");
                importViewModel.isInvalidated = true;
            }
        });

        // preview parsing finished, update view
        importViewModel.getPreviewData().observe(this, previewData -> {
            if(previewData != null){
                // reset import list mode, change of available modes
                importListMode = null;
                importViewModel.isInvalidated = false;
                Log.d(TAG,"set parsing invalidated to: false");
                refreshView();
            }
        });

        // importing progress dialog
        importViewModel.getImportingHandle().observe(this,importing -> {
            if(importing != null){
                if(importing) {
                    if (progressDialogImport == null) {
                        progressDialogImport = ProgressDialog.newInstance();
                    }
                    progressDialogImport.setDisplayMode(false,
                            importViewModel.getPreviewParser().getAmountRows(),R.string.Import_Importing_Title,
                            importViewModel.getCancelImportHandle());

                    progressDialogImport.setProgressHandle(importViewModel.getProgressData());
                    if(!progressDialogImport.isAdded())
                        progressDialogImport.show(getACActivity().getSupportFragmentManager(),ProgressDialog.TAG);
                } else if(progressDialogImport != null && progressDialogImport.isAdded()){
                    progressDialogImport.dismiss();
                    progressDialogImport = null;
                }
            }
        });

        // preview-parsing progress dialog
        importViewModel.getReparsingHandle().observe(this,reparsing -> {
            if(reparsing != null){
                if(reparsing) {
                    if (progressDialogReparse == null) {
                        progressDialogReparse = ProgressDialog.newInstance();
                    }
                    progressDialogReparse.setDisplayMode(true,0,R.string.Import_Preview_Update_Title,
                            importViewModel.getCancelPreviewHandle());

                    progressDialogReparse.setProgressHandle(importViewModel.getProgressData());
                    if(!progressDialogReparse.isAdded())
                        progressDialogReparse.show(getACActivity().getSupportFragmentManager(),ProgressDialog.TAG);
                } else if(progressDialogReparse != null && progressDialogReparse.isAdded()){
                    progressDialogReparse.dismiss();
                    progressDialogReparse = null;
                }
            }
        });

        importViewModel.getCancelPreviewHandle().observe(this, data -> {
            if(data!= null && data){
                importViewModel.isInvalidated = true;
                refreshView();
                Toast.makeText(getContext(),R.string.Import_Cancel_Toast_Preview,Toast.LENGTH_LONG).show();
                bReparse.setVisibility(View.VISIBLE);
            }
        });
    }

    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView restore: "+(savedInstanceState != null));
        View view = inflater.inflate(R.layout.fragment_import, container, false);
        setHasOptionsMenu(true);
        //initDialogs(savedInstanceState);

        singleLayout = view.findViewById(R.id.cImportNonMultilist);
        tInfo = view.findViewById(R.id.tImportInfo);
        etList = view.findViewById(R.id.tImportList);
        bSelectList = view.findViewById(R.id.bImportSelectList);
        etFile = view.findViewById(R.id.tImportPath);
        bImportOk = view.findViewById(R.id.bImportOk);
        spFormat = view.findViewById(R.id.spImportFormat);
        TextView tMsg = view.findViewById(R.id.tImportMsg);
        rgMulti = view.findViewById(R.id.rgImportMultiple);
        rgRaw = view.findViewById(R.id.rgImportSingleRaw);
        rgSingle = view.findViewById(R.id.rgImportSingleMetadata);
        bReparse = view.findViewById(R.id.bImportReparse);

        bSelectList.setOnClickListener(v -> selectList());

        Button bSelectFile = view.findViewById(R.id.bImportFile);
        bSelectFile.setOnClickListener(v -> selectFile());
        bReparse.setOnClickListener(v -> refreshParsing());

        tMsg.setMovementMethod(LinkMovementMethod.getInstance());

        initSpinner();

        bImportOk.setEnabled(false);
        etList.setKeyListener(null); // disable input
        etFile.setKeyListener(null);

        mp = new ImportFetcher.MessageProvider(this);
        if (savedInstanceState != null) {
            String path = savedInstanceState.getString(KEY_FILE_PATH, null);
            targetList = savedInstanceState.getParcelable(KEY_LIST_TARGET);
            selectedFile = savedInstanceState.getParcelable(KEY_URI);
            if (targetList != null)
                updateTargetListUI();
            // viewmodel destroyed, reparsing required
            if(importViewModel.getPreviewParser() == null && activity.getSelectedFile() != null){
                Log.d(TAG,"reparsing, no previewparser in viewmodel");
                refreshParsing();
            }
        }
        refreshView();

        // init after initial set, triggers listener before world init otherwise
        bImportOk.setOnClickListener(v -> onImport());
        ViewCreation.initRadioGroup(rgMulti, element -> {
            updateImportListModeByButtonID(element.getId());
            refreshView();
            return null;
        });
        ViewCreation.initRadioGroup(rgRaw, element -> {
            updateImportListModeByButtonID(element.getId());
            refreshView();
            return null;
        });
        ViewCreation.initRadioGroup(rgSingle, element -> {
            updateImportListModeByButtonID(element.getId());
            refreshView();
            return null;
        });

        formatViewModel.getInFormatFragmentLD().observe(getViewLifecycleOwner(), inFormatView -> {
            if(importViewModel.isInvalidated && inFormatView != null && !inFormatView) {
                refreshParsing();
            }
        });

        return view;
    }

    /**
     * Set new import list mode based on RadioButton ID
     * @param id RadioButton
     */
    private void updateImportListModeByButtonID(@IdRes int id){
        switch (id) {
            case R.id.radio_add_s:
            case R.id.radio_add_m:
            case R.id.radio_merge_r:
                importListMode = Importer.IMPORT_LIST_MODE.ADD;
                break;
            case R.id.radio_create_s:
            case R.id.radio_create_r:
                importListMode = Importer.IMPORT_LIST_MODE.CREATE;
                break;
            case R.id.radio_replace_s:
            case R.id.radio_replace_m:
                importListMode = Importer.IMPORT_LIST_MODE.REPLACE;
                break;
            case R.id.radio_ignore_m:
                importListMode = Importer.IMPORT_LIST_MODE.IGNORE;
                break;
            default:
                Log.wtf(TAG,"invalid id: "+id);
            case -1: // nothing selected
                importListMode = null;
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getFragmentActivity().finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if(getActivity() instanceof ExImportActivity) {
            activity = (ExImportActivity) getActivity();
        } else {
            throw new ClassCastException("parent Activity has to be ExImportActivity, is "+getActivity().getClass());
        }
        activity.getSelectedFile().observe(this, new Observer<Uri>() {
            @Override
            public void onChanged(Uri uri) {
                if (uri != null && !uri.equals(selectedFile)) {
                    Log.d(TAG, "got file:" + uri);
                    selectedFile = uri;
                    etFile.setText(StorageUtils.getUriName(requireContext(),uri));
                    checkInput();
                    refreshParsing();
                }
            }
        });

    }

    /**
     * Setup spinners
     */
    private void initSpinner() {
        spAdapterFormat = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item);
        spAdapterFormat.setDropDownViewResource(android.R.layout.select_dialog_singlechoice);
        SharedPreferences settings = getActivity().getSharedPreferences(PREFS_NAME, 0);
        customFormatEntry = activity.populateFormatSpinnerAdapter(spAdapterFormat);

        rgMulti.check(settings.getInt(P_KEY_I_IMP_MULTI,-1));
        rgRaw.check(settings.getInt(P_KEY_I_IMP_RAW,-1));
        rgSingle.check(settings.getInt(P_KEY_I_IMP_SINGLE,-1));

        spFormat.setAdapter(spAdapterFormat);

        spFormat.setSelection(settings.getInt(P_KEY_I_IMP_FORMAT, 0));

        spFormat.setOnItemSelectedListener(new CustomItemSelectedListener() {
            @Override
            public void itemSelected(AdapterView<?> parent, View view, int position, long id) {
                refreshParsing();
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();

        SharedPreferences settings = getActivity().getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(P_KEY_I_IMP_MULTI, rgMulti.getCheckedRadioButtonId());
        editor.putInt(P_KEY_I_IMP_SINGLE, rgSingle.getCheckedRadioButtonId());
        editor.putInt(P_KEY_I_IMP_RAW, rgRaw.getCheckedRadioButtonId());
        editor.putInt(P_KEY_I_IMP_FORMAT, spFormat.getSelectedItemPosition());
        editor.apply();
    }

    /**
     * Returns the selected CSVCustomFormat
     *
     * @return CSVCustomFormat to be used to parsing
     */
    private CSVCustomFormat getFormatSelected() {
        return spAdapterFormat.getItem(spFormat.getSelectedItemPosition()).getObject();
    }

    /**
     * Refresh preview parsing, change view accordingly
     */
    private void refreshParsing() {
        Log.d(TAG,"refreshParsing");
        if (selectedFile != null && !importViewModel.isReparsing()) {
            importViewModel.resetPreviewData();
            bReparse.setVisibility(View.GONE);
            importViewModel.previewParse(getFormatSelected(),selectedFile,requireContext(),mp);
        }
    }

    /**
     * Called when import was clicked
     */
    public void onImport() {
        Importer dataHandler = new Importer(getActivity().getApplicationContext(), importViewModel.getPreviewParser(), importListMode, targetList);
        importViewModel.runImport(dataHandler,getFormatSelected(),selectedFile,requireContext(),mp);
    }

    /**
     * Refresh visibility of all options based on the input<br>
     * also calls checkInput
     */
    private void refreshView() {
        Log.d(TAG,"refreshView");
        Log.d(TAG,"importListMode: "+importListMode);
        singleLayout.setVisibility(importViewModel.isMultiList() ? View.GONE : View.VISIBLE);
        rgMulti.setVisibility(importViewModel.isMultiList() ? View.VISIBLE : View.GONE);
        rgRaw.setVisibility(importViewModel.isRawData() ? View.VISIBLE : View.GONE);
        // last option has to watch out for invalid parsing, when others are not true would default to visible
        rgSingle.setVisibility(importViewModel.isRawData() || importViewModel.isInvalidated ? View.GONE : View.VISIBLE);
        if(selectedFile != null && importListMode == null) {
            if (rgMulti.getVisibility() == View.VISIBLE) {
                updateImportListModeByButtonID(rgMulti.getCheckedRadioButtonId());
            } else if (rgSingle.getVisibility() == View.VISIBLE) {
                updateImportListModeByButtonID(rgSingle.getCheckedRadioButtonId());
            } else if (rgRaw.getVisibility() == View.VISIBLE) {
                updateImportListModeByButtonID(rgRaw.getCheckedRadioButtonId());
            }
        }
        if (!importViewModel.isMultiList()) {
            Log.d(TAG,"non multilist, isRawData: "+importViewModel.isRawData());
            boolean hideListSelect = !importViewModel.isRawData() && importListMode != Importer.IMPORT_LIST_MODE.CREATE;
            Log.d(TAG,"hide List select: "+hideListSelect);
            etList.setVisibility(hideListSelect ? View.GONE : View.VISIBLE);
            bSelectList.setVisibility(hideListSelect ? View.GONE : View.VISIBLE);
        }

        int text;
        if (importViewModel.isRawData())
            text = R.string.Import_Info_rawlist;
        else if (importViewModel.isMultiList())
            text = R.string.Import_Info_multilist;
        else
            text = R.string.Import_Info_singlelist;
        tInfo.setText(text);

        // init, no import file
        if(selectedFile == null) {
            rgMulti.setVisibility(View.INVISIBLE);
        }
        tInfo.setVisibility(selectedFile == null ? View.INVISIBLE : View.VISIBLE);

        checkInput();
    }

    /**
     * Called on file select click
     */
    public void selectFile() {
        activity.openFile();
    }

    /**
     * Update ui with current targetList
     */
    private void updateTargetListUI() {
        etList.setText(targetList.getName());
    }

    /**
     * Set listEditorDialog actions, used by creation & on viewport restore
     */
    private void setListEditorAction() {
        Callable<Void> callable = () -> {
            updateTargetListUI();
            checkInput();
            listEditorDialog = null;
            return null;
        };
        listEditorDialog.setOkAction(callable);
        listEditorDialog.setCancelAction(() -> {
            listEditorDialog = null; // cleanup
            return null;
        });
    }

    /**
     * Called on list select click
     */
    public void selectList() {
        if (importListMode == Importer.IMPORT_LIST_MODE.CREATE) {
            if(targetList == null || targetList.isExisting()) { // could be existing list from "merge"
                targetList = VList.Companion.blank("", "", "");
                etList.setText("");
            }
            listEditorDialog = VListEditorDialog.newInstance(true);
            setListEditorAction();
            listEditorDialog.setTargetFragment(this,0);
            listEditorDialog.show(getFragmentManager(),VListEditorDialog.TAG);
        } else {
            if(targetList != null && !targetList.isExisting()) { // could be new list from "create"
                targetList = null;
                etList.setText("");
            }
            Intent myIntent = new Intent(getActivity(), ListActivity.class);
            myIntent.putExtra(ListActivity.PARAM_MULTI_SELECT, false);
            myIntent.putExtra(ListActivity.PARAM_FULL_FEATURESET,false);
            myIntent.putExtra(ListActivity.PARAM_SELECTED, targetList);
            startActivityForResult(myIntent, REQUEST_LIST_SELECT_CODE);
        }
    }

    /**
     * Verify user input and enable import button if appropriate
     */
    private void checkInput() {
        boolean is_ok = true;
        if (selectedFile == null || importListMode == null || importViewModel.isInvalidated) {
            is_ok = false;
        }
        //noinspection StatementWithEmptyBody
        if (importViewModel.isMultiList()) {
            //don't check the rest
        } else if (importViewModel.isRawData() && targetList == null) {
            is_ok = false;
        } else if (importListMode == Importer.IMPORT_LIST_MODE.CREATE) { // single list
            if (targetList == null) {
                is_ok = false;
            } else if (targetList.isExisting()) {
                is_ok = false;
            }
        } else if (importViewModel.isRawData() && (importListMode == Importer.IMPORT_LIST_MODE.ADD || importListMode == Importer.IMPORT_LIST_MODE.REPLACE) && !targetList.isExisting()) {
            if (!targetList.isExisting()) {
                is_ok = false;
            }
        }
        Log.d(TAG,"enable ok: "+is_ok);
        bImportOk.setEnabled(is_ok);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG,"onSaveInstanceState");
        outState.putParcelable(KEY_LIST_TARGET,targetList);
        outState.putParcelable(KEY_URI,selectedFile);
        if(progressDialogImport != null && progressDialogImport.isAdded())
            getACActivity().getSupportFragmentManager().putFragment(outState, TAG_PROGRESS_IMPORT, progressDialogImport);
        if(progressDialogReparse != null && progressDialogReparse.isAdded())
            getACActivity().getSupportFragmentManager().putFragment(outState, TAG_PROGRESS_REPARSE, progressDialogReparse);
        if(listEditorDialog != null && listEditorDialog.isAdded())
            getACActivity().getSupportFragmentManager().putFragment(outState,VListEditorDialog.TAG, listEditorDialog);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_LIST_SELECT_CODE) {
            Log.d(TAG, "got list");
            targetList = data.getParcelableExtra(ListActivity.RETURN_LISTS);
            etList.setText(targetList.getName());
            checkInput();
        }
    }

    @NonNull
    @Override
    public VList getList() {
        return targetList;
    }
}
