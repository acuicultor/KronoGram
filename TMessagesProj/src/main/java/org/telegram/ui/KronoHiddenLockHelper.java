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
        builder.setTitle("Proteger chats y carpetas ocultos");
        builder.setMessage("Configura un método de acceso (biometría, PIN o contraseña) antes de ocultar chats o carpetas.");
        builder.setPositiveButton("Configurar", (d, w) -> {
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
                        String msg = !TextUtils.isEmpty(errString) ? errString.toString() : "Error de autenticación biométrica";
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
                negText = KronoConfig.hiddenLockType == KronoConfig.HIDDEN_LOCK_PIN ? "Usar PIN" : "Usar contraseña";
            } else {
                negText = LocaleController.getString(R.string.Cancel);
            }
            BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle(TextUtils.isEmpty(title) ? "Bóveda oculta" : title)
                    .setSubtitle(TextUtils.isEmpty(subtitle) ? "Verifica tu identidad para continuar" : subtitle)
                    .setNegativeButtonText(negText)
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.BIOMETRIC_WEAK)
                    .build();
            prompt.authenticate(promptInfo);
        } catch (Throwable t) {
            FileLog.e(t);
            if (hasFallbackPasscode && onFallback != null) {
                onFallback.run();
            } else {
                Toast.makeText(activity, "Biometría no disponible en este momento", Toast.LENGTH_SHORT).show();
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
        editText.setHint(isPin ? "Introduce tu PIN" : "Introduce tu contraseña");
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
        builder.setTitle(isPin ? "PIN de Bóveda oculta" : "Contraseña de Bóveda oculta");
        builder.setView(container);
        builder.setPositiveButton("Desbloquear", null);
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        if (KronoConfig.hiddenUseBiometric && canUseBiometrics(activity) && activity instanceof FragmentActivity) {
            builder.setNeutralButton("Biometría", null);
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
                errorView.setText(isPin ? "PIN incorrecto" : "Contraseña incorrecta");
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
                authenticate(fragment, "Bóveda oculta", "Verifica tu identidad para continuar", onSuccess, onCancel);
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
            authenticate(fragment, "Seguridad de la bóveda", "Confirma tu identidad para cambiar el bloqueo", () -> showSetupOptionsInternal(fragment, onUpdated));
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

        labels.add("PIN numérico (4–8 dígitos)");
        actions.add(() -> showSetSecretDialog(fragment, KronoConfig.HIDDEN_LOCK_PIN, onUpdated));

        labels.add("Contraseña personalizada");
        actions.add(() -> showSetSecretDialog(fragment, KronoConfig.HIDDEN_LOCK_PASSWORD, onUpdated));

        if (bioAvailable) {
            labels.add(hasPass ? "Usar solo biometría (quitar PIN/contraseña)" : "Solo biometría del dispositivo");
            actions.add(() -> {
                KronoConfig.clearHiddenPasscode();
                KronoConfig.setHiddenUseBiometric(true);
                BulletinFactory.of(fragment).createSimpleBulletin(R.raw.contact_check, "Desbloqueo biométrico activado").show();
                if (onUpdated != null) {
                    onUpdated.run();
                }
            });
        }

        if (hasPass || (bioAvailable && KronoConfig.hiddenUseBiometric)) {
            labels.add("Desactivar bloqueo");
            actions.add(() -> {
                KronoConfig.clearHiddenPasscode();
                KronoConfig.setHiddenUseBiometric(false);
                BulletinFactory.of(fragment).createSimpleBulletin(R.raw.contact_check, "Bloqueo de la bóveda desactivado").show();
                if (onUpdated != null) {
                    onUpdated.run();
                }
            });
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity, fragment.getResourceProvider());
        builder.setTitle("Método de bloqueo de la bóveda");
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
        firstInput.setHint(isPin ? "Nuevo PIN (mínimo 4 dígitos)" : "Nueva contraseña (mínimo 4 caracteres)");
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
        confirmInput.setHint(isPin ? "Repite el PIN" : "Repite la contraseña");
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
        builder.setTitle(isPin ? "Configurar PIN" : "Configurar contraseña");
        builder.setView(container);
        builder.setPositiveButton(LocaleController.getString(R.string.Save), null);
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);

        AlertDialog dialog = builder.create();
        fragment.showDialog(dialog);

        Runnable saveAction = () -> {
            String v1 = firstInput.getText() != null ? firstInput.getText().toString() : "";
            String v2 = confirmInput.getText() != null ? confirmInput.getText().toString() : "";
            if (v1.length() < 4) {
                errorView.setText(isPin ? "El PIN debe tener al menos 4 dígitos" : "La contraseña debe tener al menos 4 caracteres");
                errorView.setVisibility(View.VISIBLE);
                AndroidUtilities.shakeView(firstInput);
                return;
            }
            if (!v1.equals(v2)) {
                errorView.setText("No coinciden, inténtalo de nuevo");
                errorView.setVisibility(View.VISIBLE);
                confirmInput.setText("");
                AndroidUtilities.shakeView(confirmInput);
                return;
            }
            KronoConfig.setHiddenPasscode(lockType, v1);
            AndroidUtilities.hideKeyboard(confirmInput);
            dialog.dismiss();
            BulletinFactory.of(fragment).createSimpleBulletin(R.raw.contact_check, isPin ? "PIN guardado" : "Contraseña guardada").show();
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
