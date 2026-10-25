"""Import/check the MiPush receive/click code without rewriting its algorithms."""

import argparse
import difflib
import hashlib
import json
import re
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT.parent / "New-Mipush-Enhance/app/src/main/java/io/github/codecodegogogo/newmipushenhance/mytest.java"
TARGET = ROOT / "app/src/main/java/com/aistra/hail/xposed/MiPushNotificationHook.java"
MANIFEST = ROOT / "app/src/test/resources/mipush-reference.json"

AUTO_METHODS = set("""
hookAutoFreezeWake hookPowerManagerScreenOffForAutoFreeze hookPowerManagerScreenOffMethod
hookPowerNotifierScreenOffForAutoFreeze hookPowerNotifierScreenOffClass hookPowerNotifierScreenOffMethod
hookBatteryStatsInteractiveForAutoFreeze hookBatteryStatsInteractiveClass
hookTaskRemovedForAutoFreeze hookActivityTaskRemoveForAutoFreeze hookActivityTaskRemoveMethod hookTaskRemovedMethod
rememberTemporaryThawedPackage handleScreenOffBroadcast handleScreenOffPowerEvent handleModuleSettingsBroadcast
containsIntentAction findIntentByAction freezeTemporaryThawedPackagesFromTask freezeTemporaryThawedPackagesFromTaskId
freezeTemporaryThawedPackagesForStrategy findTemporaryThawedPackages freezeTemporaryThawedPackage disablePackageForAutoFreeze
collectPackagesFromTask collectPackageFromComponentField collectPackageFromIntentField collectPackageFromIntentMethod
readUserIdFromTask readTaskIdFromArgs findFirstIntegerArg findFirstBooleanArg findTaskById
isAutoFreezeEnabled getFreezeStrategy readAutoFreezeEnabledFromPackageContext readFreezeStrategyFromPackageContext
getModuleSettingsFromPackageContext getModulePreferences
markRecentlyAutoFrozenPackage hasRecentlyAutoFrozenPackage pruneRecentlyAutoFrozenPackagesLocked
""".split())

AUTO_FIELDS = """
PREFS_NAME ACTION_SETTINGS_CHANGED KEY_AUTO_FREEZE_ENABLED KEY_FREEZE_STRATEGY
AUTO_FREEZE_WAKE_SUPPRESS_WINDOW_MS SCREEN_OFF_AUTO_FREEZE_THROTTLE_MS
FREEZE_STRATEGY_TASK_REMOVED FREEZE_STRATEGY_SCREEN_OFF
temporaryThawLock temporaryThawedPackages autoFreezeSuppressLock recentlyAutoFrozenPackages
autoFreezeEnabledOverride freezeStrategyOverride lastScreenOffAutoFreezeAt modulePreferences
""".split()

ADAPTED_METHODS = {
    "handleLoadPackage": "Do not install auto-freeze hooks.",
    "hookBroadcastClass": "Remove auto-freeze settings and screen-off dispatch only.",
    "wakePackagesForRecentNotificationLaunch": "Remove the unused auto-freeze suppression check.",
    "enablePackageIfDisabled": "Remove auto-freeze tracking and its log fields only.",
    "log": "Use a distinct Freeze log prefix.",
}

METHOD = re.compile(r"(?m)^    (?:private|public|protected) (?:static )?[\w<>, ?\[\]]+ (\w+)\(")


def block_end(text, start):
    depth, quote, line_comment, block_comment = 0, None, False, False
    i = start
    while i < len(text):
        c = text[i]
        pair = text[i:i + 2]
        if line_comment:
            line_comment = c != "\n"
        elif block_comment:
            if pair == "*/":
                block_comment = False
                i += 1
        elif quote:
            if c == "\\":
                i += 1
            elif c == quote:
                quote = None
        elif pair == "//":
            line_comment = True
            i += 1
        elif pair == "/*":
            block_comment = True
            i += 1
        elif c in "\"'":
            quote = c
        elif c == "{":
            depth += 1
        elif c == "}":
            depth -= 1
            if depth == 0:
                return i + 1
        i += 1
    raise ValueError("Unterminated Java block")


def methods(text):
    counts = defaultdict(int)
    for match in METHOD.finditer(text):
        name = match[1]
        opening = text.index("{", match.end())
        end = block_end(text, opening)
        counts[name] += 1
        yield name, f"{name}#{counts[name]}", match.start(), end, text[match.start():end]


def normalize_gate(text):
    return (text.replace("new MiPushToggleHook()", "new XC_MethodHook()")
            .replace("void beforeEnabled(", "void beforeHookedMethod(")
            .replace("void afterEnabled(", "void afterHookedMethod(").strip())


def replace_once(text, old, new):
    if text.count(old) != 1:
        raise ValueError(f"Reference changed; expected one occurrence: {old[:100]!r}")
    return text.replace(old, new, 1)


def port(reference):
    text = reference
    ranges = [(start, end) for name, _, start, end, _ in methods(text) if name in AUTO_METHODS]
    found = {name for name, _, _, _, _ in methods(text) if name in AUTO_METHODS}
    if found != AUTO_METHODS:
        raise ValueError(f"Reference auto-freeze methods changed: {found ^ AUTO_METHODS}")
    for start, end in sorted(ranges, reverse=True):
        text = text[:start] + text[end:]
    for field in AUTO_FIELDS:
        pattern = rf"(?m)^    private [^;\n]*\b{field}\b[^;]*;\n"
        text, count = re.subn(pattern, "", text)
        if count != 1:
            raise ValueError(f"Reference field changed: {field}")
    start = text.index("    private static class PackageUserKey {")
    end = block_end(text, text.index("{", start))
    text = text[:start] + text[end:]

    text = replace_once(text, "package io.github.codecodegogogo.newmipushenhance;",
                        "package com.aistra.hail.xposed;\n\nimport com.aistra.hail.BuildConfig;")
    text = replace_once(text, "public class mytest implements IXposedHookLoadPackage {",
                        "// Copied from New-Mipush-Enhance/mytest.java; verify with tools/mipush_reference.py.\n"
                        "public class MiPushNotificationHook implements IXposedHookLoadPackage {")
    text = replace_once(text,
                        'private static final String MODULE_PACKAGE = "io.github.codecodegogogo.newmipushenhance";',
                        'private static final String MODULE_PACKAGE = BuildConfig.APPLICATION_ID;')
    text = text.replace("import android.content.SharedPreferences;\n", "")
    text = text.replace("import de.robv.android.xposed.XSharedPreferences;\n", "")
    text = replace_once(text, "            hookAutoFreezeWake(loadPackageParam.classLoader);\n", "")
    text = text.replace("                    handleModuleSettingsBroadcast(param.args);\n", "")
    text = text.replace("                    handleScreenOffBroadcast(param.args, classLoader);\n", "")
    text = replace_once(text,
                        '        if (hasRecentlyAutoFrozenPackage(packages, userId)) {\n'
                        '            log("skip recent notification wake because package was just auto-frozen packages="\n'
                        '                    + packages + " userId=" + userId);\n'
                        '            return new WakeRequest(false, packages, userId);\n'
                        '        }\n', "")
    text = replace_once(text,
                        '            boolean remembered = rememberTemporaryThawedPackage(packageName, userId, classLoader);\n'
                        '            log("enabled disabled package before launch: " + packageName\n'
                        '                    + " userId=" + userId\n'
                        '                    + " autoFreezeEnabled=" + isAutoFreezeEnabled(classLoader)\n'
                        '                    + " freezeStrategy=" + getFreezeStrategy(classLoader)\n'
                        '                    + " remembered=" + remembered);',
                        '            log("enabled disabled package before launch: " + packageName\n'
                        '                    + " userId=" + userId);')
    text = replace_once(text, 'XposedBridge.log("NewMipushEnhance: " + message);',
                        'XposedBridge.log("Freeze-MiPush/reference: " + message);')
    text = (text.replace("new XC_MethodHook()", "new MiPushToggleHook()")
            .replace("void beforeHookedMethod(", "void beforeEnabled(")
            .replace("void afterHookedMethod(", "void afterEnabled("))
    return re.sub(r"\n{3,}", "\n\n", text)


def provenance(reference, copied):
    expected = {}
    for name, key, _, _, body in methods(reference):
        if name not in AUTO_METHODS and name not in ADAPTED_METHODS:
            expected[key] = hashlib.sha256(body.strip().encode()).hexdigest()
    actual = {key: normalize_gate(body) for _, key, _, _, body in methods(copied)}
    for key, digest in expected.items():
        if key not in actual or hashlib.sha256(actual[key].encode()).hexdigest() != digest:
            raise ValueError(f"Unexpected change to reference method: {key}")
    return {
        "source": "New-Mipush-Enhance/app/src/main/java/io/github/codecodegogogo/newmipushenhance/mytest.java",
        "sourceSha256": hashlib.sha256(reference.encode()).hexdigest(),
        "directlyCopiedMethods": expected,
        "adaptedMethods": ADAPTED_METHODS,
        "removedAutoFreezeMethods": sorted(AUTO_METHODS),
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--reference", type=Path, default=SOURCE)
    parser.add_argument("--write", action="store_true", help="Regenerate the port and its provenance fixture")
    args = parser.parse_args()
    reference = args.reference.read_text(encoding="utf-8")
    copied = port(reference)
    manifest = json.dumps(provenance(reference, copied), ensure_ascii=True, indent=2) + "\n"
    if args.write:
        TARGET.parent.mkdir(parents=True, exist_ok=True)
        MANIFEST.parent.mkdir(parents=True, exist_ok=True)
        TARGET.write_text(copied, encoding="utf-8")
        MANIFEST.write_text(manifest, encoding="utf-8")
    else:
        current = TARGET.read_text(encoding="utf-8")
        if current != copied:
            print("".join(difflib.unified_diff(copied.splitlines(True), current.splitlines(True),
                                              fromfile="reference port", tofile=str(TARGET))))
            raise SystemExit("Port differs from the reference")
        if MANIFEST.read_text(encoding="utf-8") != manifest:
            raise SystemExit("Reference provenance fixture differs")
    data = json.loads(manifest)
    print(f"PASS: {len(data['directlyCopiedMethods'])} reference methods are unchanged.")
    print("Only package/class identity, switch wrapper, log prefix and auto-freeze removal are adapted.")
    print(f"Reference SHA-256: {data['sourceSha256']}")


if __name__ == "__main__":
    main()
