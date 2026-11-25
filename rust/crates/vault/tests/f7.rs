/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! Fixture F7: what v1's `normalizeAnswer` made of answers on Android itself, on API 26 (ICU 58,
//! Unicode 9.0) and API 36 (ICU 76, Unicode 16.0), against `normalize_answer`
//! (`docs/specs/android-v1-formats.md`, invariant 5 and open questions 2 and 3). How the files were
//! made is in `tests/fixtures/README.md`.

use std::collections::BTreeMap;
use std::error::Error;
use std::fs;

use denarii_vault::normalize_answer;

type TestResult<T> = Result<T, Box<dyn Error>>;

/// How many differences a failing test lists.
const SHOWN: usize = 40;

/// One API level's recordings: what each code point that changes became, and the runs of code
/// points that the device's Unicode data doesn't assign.
struct Device {
    changed: BTreeMap<u32, String>,
    unassigned: Vec<(u32, u32)>,
}

impl Device {
    fn load(name: &str) -> TestResult<Self> {
        let mut changed = BTreeMap::new();
        for line in data_lines(&format!("v1-f7-{name}-code-points.tsv"))? {
            let (from, to) = line.split_once('\t').ok_or("a line without a tab")?;
            let from = text(from)?;
            let mut chars = from.chars();
            let c = chars.next().ok_or("an empty code point")?;
            if chars.next().is_some() {
                return Err(format!("{line:?} changes more than one code point").into());
            }
            changed.insert(u32::from(c), text(to)?);
        }
        let mut unassigned = Vec::new();
        for line in data_lines(&format!("v1-f7-{name}-unassigned.txt"))? {
            let (start, end) = line.split_once("..").ok_or("a run without `..`")?;
            unassigned.push((code(start)?, code(end)?));
        }
        Ok(Self {
            changed,
            unassigned,
        })
    }

    /// Whether the device's Unicode data assigns `c`.
    fn knows(&self, c: char) -> bool {
        let code_point = u32::from(c);
        let runs_before = self
            .unassigned
            .partition_point(|&(start, _)| start <= code_point);
        runs_before
            .checked_sub(1)
            .and_then(|run| self.unassigned.get(run))
            .is_none_or(|&(_, end)| end < code_point)
    }

    /// What v1 made of `c` alone on this device.
    fn normalized(&self, c: char) -> String {
        self.changed
            .get(&u32::from(c))
            .cloned()
            .unwrap_or_else(|| c.to_string())
    }

    /// The characters the device knows that the port normalizes differently, each with v1's form
    /// and the port's.
    fn differences(&self) -> Vec<String> {
        every_char()
            .filter(|&c| self.knows(c))
            .filter_map(|c| {
                let expected = self.normalized(c);
                let actual = normalize_answer(&c.to_string()).to_string();
                (actual != expected).then(|| {
                    format!(
                        "{}: v1 {} port {}",
                        hex(&c.to_string()),
                        hex(&expected),
                        hex(&actual)
                    )
                })
            })
            .collect()
    }

    /// The characters that the device's Unicode data doesn't assign, which the port changes
    /// because it knows a newer Unicode. v1 left them as they were there, so an answer that v1
    /// stored with one of them on that device doesn't open in the port; but only a keyboard that
    /// knew more of Unicode than the device itself could have typed one.
    fn changed_by_a_newer_unicode(&self) -> Vec<char> {
        every_char()
            .filter(|&c| !self.knows(c))
            .filter(|&c| *normalize_answer(&c.to_string()) != c.to_string())
            .collect()
    }

    /// The code points that the device doesn't assign but recorded as changed: none, if the
    /// recordings are sound.
    fn unassigned_but_changed(&self) -> Vec<u32> {
        self.changed
            .keys()
            .copied()
            .filter(|&code_point| char::from_u32(code_point).is_some_and(|c| !self.knows(c)))
            .collect()
    }
}

/// The fixture's lines without its header, which names the device, its ICU, and its Unicode.
fn data_lines(name: &str) -> TestResult<Vec<String>> {
    let path = format!("{}/tests/fixtures/{name}", env!("CARGO_MANIFEST_DIR"));
    let contents = fs::read_to_string(path)?;
    let mut lines = contents.lines();
    if !lines.next().is_some_and(|header| header.starts_with("# ")) {
        return Err(format!("{name} has no header").into());
    }
    Ok(lines.map(str::to_owned).collect())
}

fn code(hex: &str) -> TestResult<u32> {
    Ok(u32::from_str_radix(hex, 16)?)
}

/// The text that space-separated hex code points spell; an empty field is empty text.
fn text(hex: &str) -> TestResult<String> {
    let mut text = String::new();
    for field in hex.split(' ').filter(|field| !field.is_empty()) {
        text.push(char::from_u32(code(field)?).ok_or("a surrogate")?);
    }
    Ok(text)
}

fn every_char() -> impl Iterator<Item = char> {
    (0..=0x10_FFFF).filter_map(char::from_u32)
}

fn hex(text: &str) -> String {
    text.chars()
        .map(|c| format!("{:04X}", u32::from(c)))
        .collect::<Vec<_>>()
        .join(" ")
}

fn first(differences: &[String]) -> String {
    differences
        .iter()
        .take(SHOWN)
        .cloned()
        .collect::<Vec<_>>()
        .join("\n")
}

#[test]
fn every_char_api_26_knows_normalizes_as_v1_did_there() {
    let device = Device::load("api26").unwrap();
    assert_eq!(device.unassigned_but_changed(), Vec::<u32>::new());
    let differences = device.differences();
    assert!(
        differences.is_empty(),
        "{} differ:\n{}",
        differences.len(),
        first(&differences)
    );
}

#[test]
fn every_char_api_36_knows_normalizes_as_v1_did_there() {
    let device = Device::load("api36").unwrap();
    assert_eq!(device.unassigned_but_changed(), Vec::<u32>::new());
    let differences = device.differences();
    assert!(
        differences.is_empty(),
        "{} differ:\n{}",
        differences.len(),
        first(&differences)
    );
}

/// The chosen strings, and the ones drawn at random, whose characters API 26 and API 36 both
/// know: each normalizes as v1 normalized it, on both levels alike.
#[test]
fn every_recorded_string_normalizes_as_v1_did() {
    let lines = data_lines("v1-f7-strings.tsv").unwrap();
    assert_eq!(lines.len(), 2_080);
    let mut differences = Vec::new();
    for line in &lines {
        let (from, to) = line.split_once('\t').unwrap();
        let actual = normalize_answer(&text(from).unwrap()).to_string();
        if actual != text(to).unwrap() {
            differences.push(format!("{from}: v1 {to} port {}", hex(&actual)));
        }
    }
    assert!(
        differences.is_empty(),
        "{} differ:\n{}",
        differences.len(),
        first(&differences)
    );
}

/// How many characters each level didn't know that the port changes: 443 on API 26, from scripts
/// and letters that Unicode 10.0 to 18.0 added, such as Georgian Mtavruli and the era sign ㋿, and
/// 117 on API 36, from Unicode 17.0 and 18.0. A Rust or `unicode-normalization` upgrade that
/// changes these counts changes how such answers normalize, which ADR-0023 asks to review before
/// the upgrade lands.
#[test]
fn characters_that_a_newer_unicode_changes_are_counted() {
    let api_26 = Device::load("api26").unwrap().changed_by_a_newer_unicode();
    assert_eq!(api_26.len(), 443);
    assert!(api_26.contains(&'\u{1C90}') && api_26.contains(&'\u{32FF}'));
    assert_eq!(
        Device::load("api36")
            .unwrap()
            .changed_by_a_newer_unicode()
            .len(),
        117
    );
}
