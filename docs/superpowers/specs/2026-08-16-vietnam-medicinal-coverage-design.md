# Design: Vietnam-first medicinal coverage (catalog, toxic mushroom, symptom search)

**Date:** 2026-08-16  
**Status:** Approved in chat; awaiting user review of this spec before implementation  
**Scope:** Cover all requested thesis/medicinal gaps with Vietnam-first copy and data

## Goals

1. Reduce wrong identify results for common VN medicinal plants and familiar flowers.
2. Make toxic mushroom warning discoverable (not only on scan).
3. Enrich medicinal plant detail with clearer disease / remedy descriptions (Vietnamese).
4. Add symptom-guided search: user says “tôi bị đau bụng” → clarifying questions → remedy suggestions + mandatory medical disclaimer.

## Non-goals

- Not a medical diagnosis or prescription product.
- No replacement for YHCT / specialty doctor visits.
- No new backend dependency for v1 (local assets + existing catalog/folk data).
- No full Dược điển dump (~355 plants) in this pass — priority list only.

## Mandatory disclaimer (symptom flow)

Show on every symptom suggestion screen (and keep existing folk/medical disclaimers):

> Đây là dự đoán theo triệu chứng, tốt nhất người bệnh nên đến BS y học cổ truyền hoặc chuyên khoa để thăm khám tốt hơn.

## Architecture (reuse existing)

```
Home extras
  ├─ Y học (MedicinalCatalogActivity) — existing
  ├─ Bài thuốc dân gian (FolkRemediesActivity) — existing
  ├─ Nấm độc (MushroomGuideActivity) — RESTORE entry
  └─ (optional chip) Triệu chứng → SymptomRemedyActivity — NEW

SearchActivity
  └─ If query matches symptom intents → SymptomRemedyActivity

thesis_plants.json + aliases in ThesisPlantCatalog
folk_remedies.json + symptom_index (new JSON or fields)
```

Prefer **local JSON + thin Activities** over new network APIs.

---

## Section A — Catalog & identify accuracy (VN priority plants)

### Must-have catalog entries (add if missing)

| VN name | Suggested plantId | Scientific / notes |
|---------|-------------------|--------------------|
| Lưỡi hổ | `luoi-ho` | *Dracaena trifasciata* / Sansevieria; education + mild care; not primary “thuốc uống” |
| Lược vàng | `luoc-vang` | *Callisia fragrans*; medicinal folk uses + caution |
| Tần dày lá | `tan-day-la` | *Peperomia pellucida*; medicinal |
| Sao nhái | `sao-nhai` | Confirm local synonym mapping in aliases (e.g. related ornamental / regional name); document chosen taxon in JSON `description` |
| Huệ | `hoa-hue` | Common lily / tuberose style entry used in VN gardens (`Polianthes` / *Lilium* — pick one primary + aliases); education group |

### Must-harden aliases (already present — expand `scientificAliases` / common names)

- Cỏ mực / nhọ nồi — *Eclipta*
- Sài đất — *Sphagneticola* / Wedelia (keep DDVN vs ornamental note)
- Ngải cứu — *Artemisia*
- Hồng — *Rosa*
- Cúc — chrysanthemum medicinal + ornamental split already; strengthen aliases
- Dâm bụt — *Hibiscus rosa-sinensis*

### Identify pipeline rules

- Prefer catalog enrich by scientific name + VN aliases over overwriting with wrong English common names.
- Keep gallery upload / Unknown soft-fail behavior already fixed (no crash on picker URI).
- Toxic heuristics remain for fungus / amanita keywords (`ThesisPlantCatalog.looksLikeToxicMushroom`).

### Disease / medicinal richness

On medicinal plants in JSON:

- Ensure `commonDiseases[].description` and `eco_treatment` are non-empty Vietnamese where missing.
- Ensure `uses` lists short VN bullet uses.
- Keep `medicalDisclaimer` on medicinal entries.

---

## Section B — Toxic mushroom discoverability

### Problem

`MushroomGuideActivity` exists but Home no longer links to it.

### Solution

1. Add Home card (or row under thesis extras): **Nấm độc** → `MushroomGuideActivity`.
2. Keep scan-time red banner + `ToxicAlertPlayer` when `isFungus` / toxic heuristics fire.
3. Guide copy stays educational: never eat wild mushrooms; when unsure treat as toxic.

### Strings

Reuse / add: `thesis_mushroom_title`, `thesis_mushroom_sub`, home card title “Nấm độc”.

---

## Section C — Symptom → clarifying questions → remedies

### Entry points

1. Home: chip/button “Hỏi theo triệu chứng” (or integrate into Search).
2. `SearchActivity`: if query matches symptom keywords → open symptom flow instead of (or before) plant name results.

### Symptom intents (v1)

| Intent id | Trigger phrases (examples) | Follow-up questions | Remedy buckets |
|-----------|----------------------------|---------------------|----------------|
| `dau-bung` | tôi bị đau bụng, đau bụng, đau dạ dày | Bạn đau thế nào? Đau ở đâu? | gợi ý chữa đau bụng |
| `so-mui` | sổ mũi, nghẹt mũi, chảy mũi | — short follow-ups optional | sổ mũi |
| `nhuc-dau` | nhức đầu, đau đầu | — | nhức đầu |
| `met-moi` | mệt mỏi, uể oải | — | mệt mỏi |
| `dau-lung` | đau lưng, nhức lưng | — | đau lưng |

UI pattern for `dau-bung` (explicit request):

1. User query / chip: “Tôi bị đau bụng”
2. Screen shows questions: “Bạn đau thế nào?” / “Đau ở đâu?” as tappable chips
3. Tap → list of folk-style suggestions (from `folk_remedies.json` filtered by symptom tags)
4. Sticky or footer disclaimer (mandatory text above)

### Data

Add `assets/symptom_remedies.json` (or extend folk entries with `symptoms: ["dau-bung", ...]`):

```json
{
  "intents": [
    {
      "id": "dau-bung",
      "triggers": ["đau bụng", "dau bung", "đau dạ dày"],
      "questions": ["Bạn đau thế nào?", "Đau ở đâu?"],
      "remedyIds": ["nghe-tieu", "..."]
    }
  ],
  "disclaimer": "Đây là dự đoán theo triệu chứng, tốt nhất người bệnh nên đến BS y học cổ truyền hoặc chuyên khoa để thăm khám tốt hơn."
}
```

Link `remedyIds` to existing `folk_remedies.json` ids; add new folk rows where gaps exist (đau bụng, sổ mũi, nhức đầu, mệt, đau lưng).

### New UI

- `SymptomRemedyActivity` (or Fragment hosted by Search): questions → remedy list → open existing folk detail / inline expand.
- No AI required for v1 matching (keyword + tagged remedies).

---

## Section D — Implementation order

1. Catalog entries + aliases for priority plants  
2. Home Nấm độc entry + smoke check toxic banner  
3. `symptom_remedies.json` + folk tags + `SymptomRemedyActivity` + Search/Home hooks  
4. Fill missing VN disease/`uses` text on medicinal priority plants  

## Success criteria

- [ ] Gallery/camera scan of priority plants resolves to correct catalog id more often (aliases present for EN/sci/VN names).
- [ ] Home shows **Nấm độc** and opens guide.
- [ ] Query “tôi bị đau bụng” shows follow-up questions then remedy list with exact disclaimer string.
- [ ] Chips/lists exist for sổ mũi, nhức đầu, mệt mỏi, đau lưng.
- [ ] No crash on Photo Picker path (regressions from prior fix stay fixed).
- [ ] All user-facing medicinal/symptom copy is Vietnamese-first.

## Risks / notes

- **Sao nhái / Huệ** naming is ambiguous regionally — pick one primary taxon and list aliases; document in plant `description`.
- Symptom remedies must stay mild / folk + caution; never claim cure.
- Keep scope to local assets to avoid server downtime blocking thesis demo.
