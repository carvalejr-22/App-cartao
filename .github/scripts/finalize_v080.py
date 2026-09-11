from pathlib import Path

p = Path('app/src/main/java/com/carlos/appcartao/ModernCardActivity.kt')
s = p.read_text()

def rep(old, new, count=1):
    global s
    if old not in s:
        raise SystemExit('Missing pattern:\n' + old[:300])
    s = s.replace(old, new, count)

rep('''    var data by remember {\n        val loaded = store.read()\n        val prepared = modernEnsureRecurringSchedule(loaded)\n        if (prepared != loaded) {\n            store.write(prepared.copy(lastModifiedMillis = System.currentTimeMillis()))\n        }\n        mutableStateOf(prepared)\n    }''', '''    var data by remember {\n        val loaded = store.read()\n        val prepared = modernEnsureRecurringSchedule(loaded)\n        val ready = if (prepared != loaded) {\n            prepared.copy(lastModifiedMillis = System.currentTimeMillis()).also(store::write)\n        } else prepared\n        mutableStateOf(ready)\n    }''')

for name in [
    'modernAddRecurringSeries',
    'modernEnsureRecurringSchedule',
    'modernSkipRecurringOccurrence',
    'modernCancelRecurringFrom',
]:
    rep(f'private fun {name}', f'internal fun {name}')

p.write_text(s)
print('Finalized v0.8.0 code')
