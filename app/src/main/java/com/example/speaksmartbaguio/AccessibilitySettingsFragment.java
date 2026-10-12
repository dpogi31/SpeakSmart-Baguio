package com.example.speaksmartbaguio;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.speaksmartbaguio.databinding.FragmentAccessibilitySettingsBinding;
import com.example.speaksmartbaguio.utils.AccessibilityPreferences;
import com.google.android.material.snackbar.Snackbar;

public class AccessibilitySettingsFragment extends Fragment {

    private FragmentAccessibilitySettingsBinding binding;

    /*
     * Prevents programmatic control updates, such as Reset,
     * from triggering the normal setting-change listeners.
     */
    private boolean updatingControls = false;

    public AccessibilitySettingsFragment() {
        // Required empty public constructor.
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentAccessibilitySettingsBinding.inflate(
                inflater,
                container,
                false
        );

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        // Set up the Back button.
        setupBackButton();

        // Load saved values before registering listeners.
        loadSavedSettings();

        setupFontSizeOptions();
        setupHighContrastSwitch();
        setupSimplifiedLayoutSwitch();
        setupResetButton();

        applySettingsToCurrentScreen();
    }

    @Override
    public void onResume() {
        super.onResume();

        /*
         * Reapply saved settings when returning to this screen.
         * This also updates controls if settings changed elsewhere.
         */
        if (binding != null) {
            loadSavedSettings();
            applySettingsToCurrentScreen();
        }
    }

    // ---------------------------------------------------------
    // BACK BUTTON
    // ---------------------------------------------------------

    private void setupBackButton() {
        binding.buttonAccessibilityBack.setOnClickListener(v -> {

            if (!isAdded()) {
                return;
            }

            NavController navController =
                    Navigation.findNavController(v);

            /*
             * Return to the previous navigation destination.
             * If navigation cannot go back, use the activity's
             * normal Android Back behavior.
             */
            if (!navController.navigateUp()) {
                requireActivity()
                        .getOnBackPressedDispatcher()
                        .onBackPressed();
            }
        });
    }

    // ---------------------------------------------------------
    // LOAD SAVED SETTINGS
    // ---------------------------------------------------------

    private void loadSavedSettings() {
        if (binding == null) {
            return;
        }

        float fontScale =
                AccessibilityPreferences.getFontScale(
                        requireContext()
                );

        updatingControls = true;

        try {
            if (fontScale
                    == AccessibilityPreferences.FONT_SCALE_LARGE) {

                binding.fontSizeLarge.setChecked(true);

            } else if (
                    fontScale
                            == AccessibilityPreferences
                            .FONT_SCALE_EXTRA_LARGE
            ) {

                binding.fontSizeExtraLarge.setChecked(true);

            } else {

                binding.fontSizeStandard.setChecked(true);
            }

            binding.switchHighContrast.setChecked(
                    AccessibilityPreferences.isHighContrastEnabled(
                            requireContext()
                    )
            );

            binding.switchSimplifiedLayout.setChecked(
                    AccessibilityPreferences.isSimplifiedLayoutEnabled(
                            requireContext()
                    )
            );

        } finally {
            updatingControls = false;
        }
    }

    // ---------------------------------------------------------
    // FONT SIZE OPTIONS
    // ---------------------------------------------------------

    private void setupFontSizeOptions() {
        binding.fontSizeRadioGroup.setOnCheckedChangeListener(
                (RadioGroup group, int checkedId) -> {

                    if (updatingControls || checkedId == -1) {
                        return;
                    }

                    float selectedScale;

                    if (checkedId == R.id.fontSizeLarge) {

                        selectedScale =
                                AccessibilityPreferences.FONT_SCALE_LARGE;

                    } else if (
                            checkedId == R.id.fontSizeExtraLarge
                    ) {

                        selectedScale =
                                AccessibilityPreferences
                                        .FONT_SCALE_EXTRA_LARGE;

                    } else {

                        selectedScale =
                                AccessibilityPreferences.FONT_SCALE_STANDARD;
                    }

                    AccessibilityPreferences.setFontScale(
                            requireContext(),
                            selectedScale
                    );

                    applySettingsToCurrentScreen();

                    showMessage("Text size updated.");
                }
        );
    }

    // ---------------------------------------------------------
    // HIGH CONTRAST
    // ---------------------------------------------------------

    private void setupHighContrastSwitch() {
        binding.switchHighContrast.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {

                    if (updatingControls) {
                        return;
                    }

                    AccessibilityPreferences.setHighContrastEnabled(
                            requireContext(),
                            isChecked
                    );

                    applySettingsToCurrentScreen();

                    showMessage(
                            isChecked
                                    ? "High contrast enabled."
                                    : "High contrast disabled."
                    );
                }
        );
    }

    // ---------------------------------------------------------
    // SIMPLIFIED LAYOUT
    // ---------------------------------------------------------

    private void setupSimplifiedLayoutSwitch() {
        binding.switchSimplifiedLayout.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {

                    if (updatingControls) {
                        return;
                    }

                    AccessibilityPreferences.setSimplifiedLayoutEnabled(
                            requireContext(),
                            isChecked
                    );

                    applySettingsToCurrentScreen();

                    showMessage(
                            isChecked
                                    ? "Simplified layout enabled."
                                    : "Simplified layout disabled."
                    );
                }
        );
    }

    // ---------------------------------------------------------
    // RESET SETTINGS
    // ---------------------------------------------------------

    private void setupResetButton() {
        binding.buttonResetAccessibility.setOnClickListener(v -> {

            if (binding == null) {
                return;
            }

            // Restore all saved accessibility preferences.
            AccessibilityPreferences.reset(requireContext());

            /*
             * Update the controls without triggering their
             * normal listeners or showing additional messages.
             */
            updatingControls = true;

            try {
                binding.fontSizeStandard.setChecked(true);
                binding.switchHighContrast.setChecked(false);
                binding.switchSimplifiedLayout.setChecked(false);

            } finally {
                updatingControls = false;
            }

            applySettingsToCurrentScreen();

            showMessage("Accessibility settings reset.");
        });
    }

    // ---------------------------------------------------------
    // APPLY SETTINGS
    // ---------------------------------------------------------

    private void applySettingsToCurrentScreen() {
        if (binding == null || !isAdded()) {
            return;
        }

        AccessibilityPreferences.applyToViewTree(
                requireContext(),
                binding.getRoot()
        );
    }

    // ---------------------------------------------------------
    // USER FEEDBACK
    // ---------------------------------------------------------

    private void showMessage(@NonNull String message) {
        if (binding == null || !isAdded()) {
            return;
        }

        Snackbar.make(
                binding.getRoot(),
                message,
                Snackbar.LENGTH_SHORT
        ).show();
    }

    // ---------------------------------------------------------
    // CLEANUP
    // ---------------------------------------------------------

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        binding = null;
    }
}
