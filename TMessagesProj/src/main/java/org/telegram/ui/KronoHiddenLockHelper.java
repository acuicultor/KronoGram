package org.telegram.ui;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.KronoConfig;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;

import java.util.concurrent.Executor;

public final class KronoHiddenLockHelper {

    private KronoHiddenLockHelper() {
    }

    public static boolean canUseBiometrics(Context context) {
        if (context == null) {
            context = ApplicationLoader.applicationContext;
        }
        if (context == null) {
            return false;
        }
        try {
            int result = BiometricManager.from(context).canAuthenticate(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.BIOMETRIC_WEAK
            );
            return result == BiometricManager.BIOMETRIC_SUCCESS;
        } catch (Throwable t) {
            FileLog.e(t);
            return false;
        }
    }

    public static boolean isProtectionConfigured(Context context) {
        KronoConfig.loadConfig();
        return KronoConfig.hasHiddenPasscode() || (KronoConfig.hiddenUseBiometric && canUseBiometrics(context));
    }

    public static void ensureProtectionConfigured(BaseFragment fragment, Runnable onReady) {
        ensureProtectionConfigured(fragment, onReady, null);
    }

    public static void ensureProtectionConfigured(BaseFragment fragment, Runnable onReady, Runnable onCancel) {
        if (fragment == null || fragment.getParentActivity() == null) {
            if (onCancel != null) {
                onCancel.run();
            }
            return;
        }
        Activity activity = fragment.getParentActivity();
        if (isProtectionConfigured(activity)) {
            if (onReady != null) {
                onReady.run();
            }
            return;
        }
        final boolean[] resolved = new boolean[1];
        AlertDialog.Builder builder = new AlertDialog.Builder(activity, fragment.getResourceProvider());
        builder.setTitle(LocaleController.getString(R.string.KronoVaultProtectTitle));
        builder.setMessage(LocaleController.getString(R.string.KronoVaultProtectMsg));
        builder.setPositiveButton(LocaleController.getString(R.string.KronoConfigure), (d, w) -> {
            resolved[0] = true;
            showSetupLockDialog(fragment, () -> {
                if (isProtectionConfigured(activity)) {
                    if (onReady != null) {
                        onReady.run();
                    }
                } else if (onCancel != null) {
                    onCancel.run();
                }
            });
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        AlertDialog dialog = builder.create();
        DialogInterface.OnDismissListener dismissListener = d -> {
            if (!resolved[0] && onCancel != null) {
                resolved[0] = true;
                onCancel.run();
            }
        };
        if (fragment.showDialog(dialog, true, dismissListener) == null && !resolved[0] && onCancel != null) {
            resolved[0] = true;
            onCancel.run();
        }
    }

    public static void authenticate(BaseFragment fragment, String title, String subtitle, Runnable onSuccess) {
        authenticate(fragment, title, subtitle, onSuccess, null);
    }

    public static void authenticate(BaseFragment fragment, String title, String subtitle, Runnable onSuccess, Runnable onCancel) {
        if (fragment == null || fragment.getParentActivity() == null) {
            if (onCancel != null) {
                onCancel.run();
            }
            return;
        }
        Activity activity = fragment.getParentActivity();
        KronoConfig.loadConfig();

        if (!isProtectionConfigured(activity)) {
            ensureProtectionConfigured(fragment, () -> {
                KronoConfig.setHiddenVaultUnlocked(true);
                if (onSuccess != null) {
                    onSuccess.run();
                }
            }, onCancel);
            return;
        }

        if (KronoConfig.isHiddenVaultUnlocked()) {
            if (onSuccess != null) {
                onSuccess.run();
            }
            return;
        }

        boolean hasBio = KronoConfig.hiddenUseBiometric && canUseBiometrics(activity);
        boolean hasPass = KronoConfig.hasHiddenPasscode();

        if (hasBio && activity instanceof FragmentActivity) {
            showBiometricPrompt((FragmentActivity) activity, title, subtitle, hasPass, () -> {
                KronoConfig.setHiddenVaultUnlocked(true);
                if (onSuccess != null) {
                    onSuccess.run();
                }
            }, () -> {
                if (hasPass) {
                    showVerifyPasscodeDialog(fragment, onSuccess, onCancel);
                } else if (onCancel != null) {
                    onCancel.run();
                }
            }, onCancel);
        } else if (hasPass) {
            showVerifyPasscodeDialog(fragment, onSuccess, onCancel);
        } else if (onCancel != null) {
            onCancel.run();
        }
    }

    private static void showBiometricPrompt(FragmentActivity activity, String title, String subtitle, boolean hasFallbackPasscode, Runnable onSuccess, Runnable onFallback, Runnable onCancel) {
        try {
            Executor executor = ContextCompat.getMainExecutor(activity);
            BiometricPrompt prompt = new BiometricPrompt(activity, executor, new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                    if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        if (hasFallbackPasscode && onFallback != null) {
                            onFallback.run();
                        } else if (onCancel != null) {
                            onCancel.run();
                        }
                        return;
                    }
                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED || errorCode == BiometricPrompt.ERROR_CANCELED) {
                        if (onCancel != null) {
                            onCancel.run();
                        }
                        return;
                    }
                    if (hasFallbackPasscode && onFallback != null) {
                        onFallback.run();
                    } else {
                        String msg = !TextUtils.isEmpty(errString) ? errString.toString() : LocaleController.getString(R.string.KronoBioAuthError);
                        Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
                        if (onCancel != null) {
                            onCancel.run();
                        }
                    }
                }

                @Override
                public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                }

                @Override
                public void onAuthenticationFailed() {
                    // System prompt handles retries; user can also switch to PIN/password via negative button.
                }
            });
            String negText;
            if (hasFallbackPasscode) {
                negText = LocaleController.getString(KronoConfig.hiddenLockType == KronoConfig.HIDDEN_LOCK_PIN ? R.string.KronoUsePin : R.string.KronoUsePass);
            } else {
                negText = LocaleController.getString(R.string.Cancel);
            }
            BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle(TextUtils.isEmpty(title) ? LocaleController.getString(R.string.KronoVaultTitle) : title)
                    .setSubtitle(TextUtils.isEmpty(subtitle) ? LocaleController.getString(R.string.KronoVaultVerifySubtitle) : subtitle)
                    .setNegativeButtonText(negText)
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.BIOMETRIC_WEAK)
                    .build();
            prompt.authenticate(promptInfo);
        } catch (Throwable t) {
            FileLog.e(t);
            if (hasFallbackPasscode && onFallback != null) {
                onFallback.run();
            } else {
                Toast.makeText(activity, LocaleController.getString(R.string.KronoBioUnavailable), Toast.LENGTH_SHORT).show();
                if (onCancel != null) {
                    onCancel.run();
                }
            }
        }
    }

    public static void showVerifyPasscodeDialog(BaseFragment fragment, Runnable onSuccess) {
        showVerifyPasscodeDialog(fragment, onSuccess, null);
    }

    public static void showVerifyPasscodeDialog(BaseFragment fragment, Runnable onSuccess, Runnable onCancel) {
        if (fragment == null || fragment.getParentActivity() == null) {
            if (onCancel != null) {
                onCancel.run();
            }
            return;
        }
        Activity activity = fragment.getParentActivity();
        boolean isPin = KronoConfig.hiddenLockType == KronoConfig.HIDDEN_LOCK_PIN;

        LinearLayout container = new LinearLayout(activity);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(8), AndroidUtilities.dp(24), AndroidUtilities.dp(4));

        EditTextBoldCursor editText = new EditTextBoldCursor(activity);
        editText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        editText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, fragment.getResourceProvider()));
        editText.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint, fragment.getResourceProvider()));
        editText.setHeaderHintColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader, fragment.getResourceProvider()));
        editText.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, fragment.getResourceProvider()));
        editText.setCursorSize(AndroidUtilities.dp(20));
        editText.setCursorWidth(1.5f);
        editText.setSingleLine(true);
        editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        editText.setHint(LocaleController.getString(isPin ? R.string.KronoEnterPin : R.string.KronoEnterPass));
        editText.setInputType(isPin
                ? (InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD)
                : (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD));
        editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
        if (isPin) {
            editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(12)});
        }
        container.addView(editText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 42));

        TextView errorView = new TextView(activity);
        errorView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        errorView.setTextColor(Theme.getColor(Theme.key_text_RedRegular, fragment.getResourceProvider()));
        errorView.setVisibility(View.GONE);
        errorView.setPadding(0, AndroidUtilities.dp(6), 0, 0);
        container.addView(errorView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        AlertDialog.Builder builder = new AlertDialog.Builder(activity, fragment.getResourceProvider());
        builder.setTitle(LocaleController.getString(isPin ? R.string.KronoVaultPinTitle : R.string.KronoVaultPassTitle));
        builder.setView(container);
        builder.setPositiveButton(LocaleController.getString(R.string.KronoUnlock), null);
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        if (KronoConfig.hiddenUseBiometric && canUseBiometrics(activity) && activity instanceof FragmentActivity) {
            builder.setNeutralButton(LocaleController.getString(R.string.KronoBiometrics), null);
        }

        final boolean[] resolved = new boolean[1];
        AlertDialog dialog = builder.create();
        DialogInterface.OnDismissListener dismissListener = d -> {
            if (!resolved[0] && onCancel != null) {
                resolved[0] = true;
                onCancel.run();
            }
        };
        if (fragment.showDialog(dialog, true, dismissListener) == null) {
            if (!resolved[0] && onCancel != null) {
                resolved[0] = true;
                onCancel.run();
            }
            return;
        }

        Runnable submitAction = () -> {
            String value = editText.getText() != null ? editText.getText().toString() : "";
            if (KronoConfig.verifyHiddenPasscode(value)) {
                resolved[0] = true;
                AndroidUtilities.hideKeyboard(editText);
                dialog.dismiss();
                KronoConfig.setHiddenVaultUnlocked(true);
                if (onSuccess != null) {
                    onSuccess.run();
                }
            } else {
                errorView.setText(LocaleController.getString(isPin ? R.string.KronoWrongPin : R.string.KronoWrongPass));
                errorView.setVisibility(View.VISIBLE);
                editText.setText("");
                AndroidUtilities.shakeView(editText);
            }
        };

        editText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submitAction.run();
                return true;
            }
            return false;
        });

        View posBtn = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (posBtn != null) {
            posBtn.setOnClickListener(v -> submitAction.run());
        }
        View neuBtn = dialog.getButton(DialogInterface.BUTTON_NEUTRAL);
        if (neuBtn != null && activity instanceof FragmentActivity) {
            neuBtn.setOnClickListener(v -> {
                resolved[0] = true;
                AndroidUtilities.hideKeyboard(editText);
                dialog.dismiss();
                authenticate(fragment, LocaleController.getString(R.string.KronoVaultTitle), LocaleController.getString(R.string.KronoVaultVerifySubtitle), onSuccess, onCancel);
            });
        }
        editText.requestFocus();
        AndroidUtilities.runOnUIThread(() -> AndroidUtilities.showKeyboard(editText), 120);
    }

    public static void showSetupLockDialog(BaseFragment fragment, Runnable onUpdated) {
        if (fragment == null || fragment.getParentActivity() == null) {
            return;
        }
        Activity activity = fragment.getParentActivity();
        if (isProtectionConfigured(activity)) {
            authenticate(fragment, LocaleController.getString(R.string.KronoVaultSecurity), LocaleController.getString(R.string.KronoVaultConfirmChange), () -> showSetupOptionsInternal(fragment, onUpdated));
        } else {
            showSetupOptionsInternal(fragment, onUpdated);
        }
    }

    private static void showSetupOptionsInternal(BaseFragment fragment, Runnable onUpdated) {
        Activity activity = fragment.getParentActivity();
        if (activity == null) {
            return;
        }
        boolean bioAvailable = canUseBiometrics(activity);
        boolean hasPass = KronoConfig.hasHiddenPasscode();

        java.util.ArrayList<String> labels = new java.util.ArrayList<>();
        java.util.ArrayList<Runnable> actions = new java.util.ArrayList<>();

        labels.add(LocaleController.getString(R.string.KronoLockOptPin));
        actions.add(() -> showSetSecretDialog(fragment, KronoConfig.HIDDEN_LOCK_PIN, onUpdated));

        labels.add(LocaleController.getString(R.string.KronoLockOptPass));
        actions.add(() -> showSetSecretDialog(fragment, KronoConfig.HIDDEN_LOCK_PASSWORD, onUpdated));

        if (bioAvailable) {
            labels.add(LocaleController.getString(hasPass ? R.string.KronoLockOptBioOnlyRemove : R.string.KronoLockOptBioDevice));
            actions.add(() -> {
                KronoConfig.clearHiddenPasscode();
                KronoConfig.setHiddenUseBiometric(true);
                BulletinFactory.of(fragment).createSimpleBulletin(R.raw.contact_check, LocaleController.getString(R.string.KronoBioEnabled)).show();
                if (onUpdated != null) {
                    onUpdated.run();
                }
            });
        }

        if (hasPass || (bioAvailable && KronoConfig.hiddenUseBiometric)) {
            labels.add(LocaleController.getString(R.string.KronoDisableLock));
            actions.add(() -> {
                KronoConfig.clearHiddenPasscode();
                KronoConfig.setHiddenUseBiometric(false);
                BulletinFactory.of(fragment).createSimpleBulletin(R.raw.contact_check, LocaleController.getString(R.string.KronoLockDisabled)).show();
                if (onUpdated != null) {
                    onUpdated.run();
                }
            });
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity, fragment.getResourceProvider());
        builder.setTitle(LocaleController.getString(R.string.KronoVaultLockTitle));
        builder.setItems(labels.toArray(new CharSequence[0]), (d, which) -> {
            if (which >= 0 && which < actions.size()) {
                actions.get(which).run();
            }
        });
        fragment.showDialog(builder.create());
    }

    private static void showSetSecretDialog(BaseFragment fragment, int lockType, Runnable onUpdated) {
        Activity activity = fragment.getParentActivity();
        if (activity == null) {
            return;
        }
        boolean isPin = lockType == KronoConfig.HIDDEN_LOCK_PIN;

        LinearLayout container = new LinearLayout(activity);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(8), AndroidUtilities.dp(24), AndroidUtilities.dp(4));

        EditTextBoldCursor firstInput = new EditTextBoldCursor(activity);
        firstInput.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
        firstInput.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, fragment.getResourceProvider()));
        firstInput.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint, fragment.getResourceProvider()));
        firstInput.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, fragment.getResourceProvider()));
        firstInput.setSingleLine(true);
        firstInput.setHint(LocaleController.getString(isPin ? R.string.KronoNewPinHint : R.string.KronoNewPassHint));
        firstInput.setInputType(isPin
                ? (InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD)
                : (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD));
        firstInput.setTransformationMethod(PasswordTransformationMethod.getInstance());
        if (isPin) {
            firstInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(8)});
        }
        container.addView(firstInput, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 42));

        EditTextBoldCursor confirmInput = new EditTextBoldCursor(activity);
        confirmInput.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
        confirmInput.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, fragment.getResourceProvider()));
        confirmInput.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint, fragment.getResourceProvider()));
        confirmInput.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, fragment.getResourceProvider()));
        confirmInput.setSingleLine(true);
        confirmInput.setImeOptions(EditorInfo.IME_ACTION_DONE);
        confirmInput.setHint(LocaleController.getString(isPin ? R.string.KronoRepeatPin : R.string.KronoRepeatPass));
        confirmInput.setInputType(firstInput.getInputType());
        confirmInput.setTransformationMethod(PasswordTransformationMethod.getInstance());
        if (isPin) {
            confirmInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(8)});
        }
        container.addView(confirmInput, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 42, 0, 10, 0, 0));

        TextView errorView = new TextView(activity);
        errorView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        errorView.setGravity(Gravity.START);
        errorView.setTextColor(Theme.getColor(Theme.key_text_RedRegular, fragment.getResourceProvider()));
        errorView.setVisibility(View.GONE);
        errorView.setPadding(0, AndroidUtilities.dp(6), 0, 0);
        container.addView(errorView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        AlertDialog.Builder builder = new AlertDialog.Builder(activity, fragment.getResourceProvider());
        builder.setTitle(LocaleController.getString(isPin ? R.string.KronoSetupPin : R.string.KronoSetupPass));
        builder.setView(container);
        builder.setPositiveButton(LocaleController.getString(R.string.Save), null);
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);

        AlertDialog dialog = builder.create();
        fragment.showDialog(dialog);

        Runnable saveAction = () -> {
            String v1 = firstInput.getText() != null ? firstInput.getText().toString() : "";
            String v2 = confirmInput.getText() != null ? confirmInput.getText().toString() : "";
            if (v1.length() < 4) {
                errorView.setText(LocaleController.getString(isPin ? R.string.KronoPinMinErr : R.string.KronoPassMinErr));
                errorView.setVisibility(View.VISIBLE);
                AndroidUtilities.shakeView(firstInput);
                return;
            }
            if (!v1.equals(v2)) {
                errorView.setText(LocaleController.getString(R.string.KronoMismatchRetry));
                errorView.setVisibility(View.VISIBLE);
                confirmInput.setText("");
                AndroidUtilities.shakeView(confirmInput);
                return;
            }
            KronoConfig.setHiddenPasscode(lockType, v1);
            AndroidUtilities.hideKeyboard(confirmInput);
            dialog.dismiss();
            BulletinFactory.of(fragment).createSimpleBulletin(R.raw.contact_check, LocaleController.getString(isPin ? R.string.KronoPinSaved : R.string.KronoPassSaved)).show();
            if (onUpdated != null) {
                onUpdated.run();
            }
        };

        confirmInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                saveAction.run();
                return true;
            }
            return false;
        });

        View posBtn = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (posBtn != null) {
            posBtn.setOnClickListener(v -> saveAction.run());
        }
        firstInput.requestFocus();
        AndroidUtilities.runOnUIThread(() -> AndroidUtilities.showKeyboard(firstInput), 120);
    }
}
