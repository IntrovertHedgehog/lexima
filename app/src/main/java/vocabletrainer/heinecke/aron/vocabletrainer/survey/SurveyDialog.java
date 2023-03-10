package vocabletrainer.heinecke.aron.vocabletrainer.survey;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProviders;
import vocabletrainer.heinecke.aron.vocabletrainer.R;

import static vocabletrainer.heinecke.aron.vocabletrainer.activity.MainActivity.PREFS_NAME;

/**
 * Dialog showing progress during import/export and preview parsing.
 * Capable of two modes: indefinite mode, displaying the actual progress data and actual progress mode<br>
 * Works with a LiveData<Integer>
 * @author Aron Heinecke
 */
public class SurveyDialog extends DialogFragment {
    public static final String TAG = "SurveyDialog";
    private static final String P_KEY_SURVEY_DIALOG_API= "showedAPISurveyDialog";
    private static final String P_KEY_SURVEY_DIALOG_API_COUNTER= "APISurveyCounter";
    private ProgressBar progressBar;
    private Button btnCancel;
    private Button btnAccept;
    private SurveyViewModel surveyViewModel;
    private TextView msg;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // required for using correct width
        setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomDialog);
        surveyViewModel = ViewModelProviders.of(getActivity()).get(SurveyViewModel.class);

        surveyViewModel.getErrorLiveData().observe(this,err -> {
            if(err != null && err){
                Toast.makeText(getContext(),R.string.Survey_Diag_Error_Toast,Toast.LENGTH_LONG).show();

                this.dismiss();
            }
        });

        surveyViewModel.getLoadingLiveData().observe(this,loading -> {
            if(loading != null && loading) {
                btnAccept.setEnabled(false);
                btnCancel.setEnabled(false);
                progressBar.setVisibility(View.VISIBLE);
            } else if ( surveyViewModel.wasRunning() ) {
                progressBar.setVisibility(View.GONE);
                SharedPreferences settings = getContext().getSharedPreferences(PREFS_NAME, 0);
                settings.edit().putBoolean(P_KEY_SURVEY_DIALOG_API, true).apply();
                Toast.makeText(getContext(), R.string.Survey_Diag_Success_Toast, Toast.LENGTH_LONG).show();
                this.dismiss();
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();
        this.setCancelable(false);
    }

    public static boolean shouldDisplaySurvey(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        if (settings.getBoolean(P_KEY_SURVEY_DIALOG_API,false)) {
            return false;
        }
        // wait 10 times till displaying the dialog
        int count = settings.getInt(P_KEY_SURVEY_DIALOG_API_COUNTER,10);
        if(count > 0) {
            settings.edit().putInt(P_KEY_SURVEY_DIALOG_API_COUNTER,count -1).apply();
        }
        return count == 0;
    }

    /**
     * Creates a new instance
     */
    public static SurveyDialog newInstance(){
        SurveyDialog dialog = new SurveyDialog();
        Bundle args = new Bundle();
        dialog.setArguments(args);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = View.inflate(getContext(),R.layout.dialog_survey, null);
        /*int color = ContextCompat.getColor(requireContext(), R.color.background);
        view.setBackgroundColor(color);*/
        progressBar = view.findViewById(R.id.dialog_progressbar);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.GONE);
        btnCancel = view.findViewById(R.id.button_decline_survey);
        btnAccept = view.findViewById(R.id.button_participate);
        msg = view.findViewById(R.id.survey_message);
        msg.setMovementMethod(LinkMovementMethod.getInstance());
        btnCancel.setOnClickListener(v -> {
            SharedPreferences settings = getContext().getSharedPreferences(PREFS_NAME, 0);
            settings.edit().putBoolean(P_KEY_SURVEY_DIALOG_API,true).apply();
            this.dismiss();
        });
        btnAccept.setOnClickListener(v -> {
            Log.d(TAG,"survey participating..");
            surveyViewModel.submitSurvey();
        });
        return view;
    }


}
