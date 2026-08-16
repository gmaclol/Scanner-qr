# Lessons Learned

## Build Gradle 9.6.0+ vs AGP 8.x
- **Errore**: Incompatibilità tra AGP 8.13.2 e Gradle 9.6+ dovuta alla rimozione dell'API interna InternalProblems.
- **Causa**: Gradle 9.6 ha rimosso API deprecate usate dai vecchi plugin Android.
- **Regola**: Mantenere Gradle alla versione 9.3.1 che è perfettamente compatibile con AGP 8.13.2.
