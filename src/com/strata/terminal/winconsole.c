/* Windows console setup for the STRATA terminal viewer.
 * Called once at startup as a static COBOL CALL (see -fstatic-call).
 * Enables VT escape processing + UTF-8 output so the viewer works on
 * legacy conhost, not just Windows Terminal. No-op safe anywhere:
 * every API result is checked, failures just return.
 */
#include <windows.h>

void enable_vt(void) {
    HANDLE h = GetStdHandle(STD_OUTPUT_HANDLE);
    if (h == NULL || h == INVALID_HANDLE_VALUE) {
        return;
    }
    DWORD mode = 0;
    if (GetConsoleMode(h, &mode)) {
        mode |= ENABLE_VIRTUAL_TERMINAL_PROCESSING;
        SetConsoleMode(h, mode);
    }
    SetConsoleOutputCP(CP_UTF8);
}
