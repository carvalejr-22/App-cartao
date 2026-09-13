# Regras específicas do projeto.
#
# O app usa AGP 9/R8 full mode. O ML Kit descobre parte dos componentes de OCR em
# tempo de execução; se essas classes forem removidas/renomeadas agressivamente,
# TextRecognition.getClient(...) pode falhar em aparelhos reais.
# Mantemos somente a família ML Kit / componentes internos necessária ao OCR.
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_** { *; }
-keep class com.google.firebase.components.** { *; }
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Mantém nomes de métodos nativos usados pelo modelo OCR empacotado.
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}
