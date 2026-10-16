import { useState, useEffect, useRef, useCallback } from "react";

import { motion, AnimatePresence, MotionConfig, animate } from "motion/react";

import { api } from "./api";

import { supabase } from "./lib/supabase";

import "./App.css";



const CATEGORIES = ["Wallet", "Keys", "ID card", "Earphones", "Phone", "Bag", "Bottle", "Other"];

const fmt = (d) => (d ? new Date(d).toLocaleString([], { dateStyle: "medium", timeStyle: "short" }) : "Time unknown");

const formatTimeGap = (minutes) => minutes < 60
  ? `${Math.max(1, minutes)} ${minutes === 1 ? "minute" : "minutes"}`
  : minutes < 1440
    ? `${Math.round(minutes / 60)} ${Math.round(minutes / 60) === 1 ? "hour" : "hours"}`
    : `${Math.round(minutes / 1440)} ${Math.round(minutes / 1440) === 1 ? "day" : "days"}`;

const chips = (m) => [

  m.descriptionScore >= 60 && "Similar wording",

  m.locationScore >= 70 && "Same area",

  m.dateTimeScore >= 70 && "Close in time",

].filter(Boolean).concat(m.descriptionScore < 60 && m.locationScore < 70 && m.dateTimeScore < 70 ? ["Weak match"] : []);

async function toPhoto(file) {

  const url = await new Promise((res, rej) => { const r = new FileReader(); r.onload = () => res(r.result); r.onerror = rej; r.readAsDataURL(file); });

  const img = await new Promise((res, rej) => { const i = new Image(); i.onload = () => res(i); i.onerror = rej; i.src = url; });

  const s = Math.min(1, 800 / Math.max(img.width, img.height));

  const c = document.createElement("canvas");

  c.width = Math.round(img.width * s); c.height = Math.round(img.height * s);

  c.getContext("2d").drawImage(img, 0, 0, c.width, c.height);

  return c.toDataURL("image/jpeg", 0.8);

}



const list = { show: { transition: { staggerChildren: 0.08 } } };

const rise = { hidden: { opacity: 0, y: 18 }, show: { opacity: 1, y: 0, transition: { type: "spring", stiffness: 160, damping: 18 } } };



function useAsync() {

  const [state, set] = useState({ loading: false, error: "" });

  const run = useCallback(async (fn) => {

    set({ loading: true, error: "" });

    try { const out = await fn(); set({ loading: false, error: "" }); return out; }

    catch (e) { set({ loading: false, error: e.message }); }

  }, []);

  return [state, run];

}

const Notice = ({ error }) => error ? <motion.p className="error" role="alert" initial={{ x: -8 }} animate={{ x: [0, -6, 6, -3, 0] }}>{error}</motion.p> : null;

function CountUp({ value }) {

  const [n, setN] = useState(0);

  useEffect(() => { const c = animate(0, value, { duration: 1.1, ease: "easeOut", onUpdate: (v) => setN(Math.round(v)) }); return () => c.stop(); }, [value]);

  return <>{n}</>;

}

function Segmented({ options, value, onChange, id }) {

  return <div className="seg" role="group">{options.map(([v, label]) => <button type="button" key={label} onClick={() => onChange(v)} className={value === v ? `on ${v.toLowerCase()}` : ""}>{value === v && <motion.span layoutId={id} className="pill" transition={{ type: "spring", stiffness: 380, damping: 32 }} />}<span className="lbl">{label}</span></button>)}</div>;

}

function Toast({ msg }) {

  return <AnimatePresence>{msg && <motion.div className="toast" role="status" initial={{ y: 60, opacity: 0 }} animate={{ y: 0, opacity: 1 }} exit={{ y: 60, opacity: 0 }}>{msg}</motion.div>}</AnimatePresence>;

}

function LoopMark({ className = "" }) {
  return <svg className={`loop-mark ${className}`} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <path d="M19 7.5A7.5 7.5 0 0 0 6.3 6.3L4 8.6m0 0V4.8m0 3.8h3.8" />
    <path d="M5 16.5a7.5 7.5 0 0 0 12.7 1.2l2.3-2.3m0 0v3.8m0-3.8h-3.8" />
  </svg>;
}

function Skeleton() {
  return <div className="skel"><LoopMark className="skel-loop" /></div>;
}


function toLocalInput(d = new Date()) {

  const pad = (n) => String(n).padStart(2, "0");

  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;

}



function DateTimeEntry({ value, onChange }) {
  const [picked, setPicked] = useState("Just now");
  const ago = (days, hour) => { const d = new Date(); d.setDate(d.getDate() - days); if (hour != null) d.setHours(hour, 0, 0, 0); return toLocalInput(d); };
  const quick = [["Just now", () => toLocalInput()], ["This morning", () => ago(0, 9)], ["Yesterday", () => ago(1, 12)], ["Last week", () => ago(7, 12)]];
  return (
    <div className="rf-group" role="group" aria-labelledby="when-label">
      <span id="when-label" className="rf-label">When did it happen?</span>
      <div className="rf-chips">
        {quick.map(([label, fn]) => (
          <button type="button" key={label} className={`rf-chip ${picked === label ? "on" : ""}`} aria-pressed={picked === label}
            onClick={() => { setPicked(label); onChange(fn()); }}>{label}</button>
        ))}
      </div>
      <input type="datetime-local" required aria-label="Exact date and time" max={toLocalInput()} value={value}
        onChange={(e) => { setPicked(""); onChange(e.target.value); }} />
    </div>
  );
}



const BtnIcon = ({ children }) => <span className="btn-ico" aria-hidden="true"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">{children}</svg></span>;

const ArrowIcon = () => <svg className="btn-arrow" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d="M5 12h14M13 6l6 6-6 6" /></svg>;

function Hero({ onReport }) {

  return <section className="hero-section" id="home">

    <div className="hero-copy">

      <p className="eyebrow"><span className="eyebrow-spark">✳</span> LOST SOMETHING? FOUND SOMETHING?</p>

      <h1>FindLoop</h1>

      <p className="hero-tagline">Every lost item deserves a way home.</p>

      <p className="hero-intro">Report it, discover possible matches, and help belongings find their way home.</p>

      <div className="hero-actions">

        <button className="primary hero-button report-lost" onClick={() => onReport("LOST")}><BtnIcon><circle cx="10.5" cy="10.5" r="6.5" /><path d="m20 20-4.9-4.9" /><path d="M9 9.2a1.8 1.8 0 1 1 2.6 1.6c-.6.3-1.1.7-1.1 1.4" /><path d="M10.5 14.1h.01" /></BtnIcon> Report Lost <ArrowIcon /></button>

        <button className="secondary hero-button report-found" onClick={() => onReport("FOUND")}><BtnIcon><path d="M12 22s7-5.6 7-11.5a7 7 0 1 0-14 0C5 16.4 12 22 12 22z" /><path d="m8.8 10.6 2.2 2.2 4-4.2" /></BtnIcon> Report Found <ArrowIcon /></button>

      </div>

      <p className="hero-reassurance"><span>✓</span> Sign in securely to report or contact an owner.</p>

    </div>

    <div className="hero-visual">

      <div className="hero-photo" role="img" aria-label="Backpack and everyday belongings on a sunny campus bench">

      </div>

      <span className="hero-scribble scribble-one">✳</span><span className="hero-scribble scribble-two">✦</span>

    </div>

  </section>;

}

const Svg = ({ children }) => <svg viewBox="0 0 24 24" width="40" height="40" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{children}</svg>;

const featureIcons = {

  search: <Svg><circle cx="11" cy="11" r="7.5" /><path d="m21 21-4.3-4.3" /><path d="M8.5 11h5M11 8.5v5" opacity=".5" /></Svg>,

  pin: <Svg><path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z" /><circle cx="12" cy="10" r="3" /></Svg>,

  shield: <Svg><path d="M12 22s8-4 8-11V5l-8-3-8 3v6c0 7 8 11 8 11Z" /><path d="m9 12 2 2 4-4" /></Svg>,

  bell: <Svg><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9" /><path d="M10 21h4" /></Svg>,

};

function FeatureHighlights() {

  const features = [

    { icon: "search", title: "Smart match suggestions", tag: "See why it matched", desc: "FindLoop compares descriptions, locations, and dates, then explains why two reports may match." },

    { icon: "pin", title: "Location-aware search", tag: "Search your campus", desc: "Search by place or browse nearby reports. Your exact GPS coordinates stay private." },

    { icon: "shield", title: "Private contact & ownership checks", tag: "Share details safely", desc: "Message through FindLoop. Found-item claimants answer a private question before contact opens." },

    { icon: "bell", title: "Match & reply notifications", tag: "Your updates, your pace", desc: "Choose whether to get match suggestions and private replies, and how often they arrive." },

  ];

  return <section className="feature-strip" aria-label="FindLoop features">{features.map((f) => <article className="feature-tile" key={f.title}><span className="feature-icon" aria-hidden="true">{featureIcons[f.icon]}</span><h3>{f.title}</h3><p>{f.desc}</p><span className="feature-tag">{f.tag}</span></article>)}</section>;

}

function HowItWorks() {
  const steps = [
    ["Report it", "Describe the item, where and when it went missing or turned up, and add a photo if you have one."],
    ["Review matches", "FindLoop scores other reports on wording, place and time, and shows why each one matched."],
    ["Send a request", "Found a likely match? Ask the other person to connect. They decide whether to accept."],
    ["Chat and hand over", "Talk in a private chat and agree on a safe public place, like a college security office."],
  ];
  return <section className="home-section" id="how-it-works" aria-labelledby="how-title">
    <div className="home-section-head"><h2 id="how-title">How FindLoop works</h2><p>From a report to a handover in four steps.</p></div>
    <div className="loop-divider" aria-hidden="true"><LoopMark /></div>
    <ol className="how-steps">{steps.map(([title, desc], i) => <li className="how-step" key={title}><span className="how-num" aria-hidden="true">{i + 1}</span><h3>{title}</h3><p>{desc}</p></li>)}</ol>
  </section>;
}

const moreIcons = {
  report: <Svg><path d="M6 3h8l4 4v14H6z" /><path d="M14 3v4h4M9 12h6M9 16h6" /></Svg>,
  photo: <Svg><rect x="3" y="5" width="18" height="14" rx="2" /><circle cx="9" cy="10" r="1.6" /><path d="m21 16-5-5-8 8" /></Svg>,
  request: <Svg><path d="M4 12l16-8-6 16-3-7z" /></Svg>,
  chat: <Svg><path d="M21 12a8 8 0 0 1-11.6 7.1L4 20l1-4.6A8 8 0 1 1 21 12z" /></Svg>,
  shield: <Svg><path d="M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6z" /><path d="m9 12 2 2 4-4" /></Svg>,
};

function MoreFeatures() {
  const items = [
    { icon: "report", title: "Lost and found reports", desc: "Post what you lost or what you found, with its category, a description, the place and the time." },
    { icon: "photo", title: "Photos on reports", desc: "Attach a photo so the owner can recognise the item at a glance. Images are resized before they upload." },
    { icon: "search", title: "Browse and search", desc: "Filter open reports by Lost or Found, and search by word or place. No sign-in needed to look around." },
    { icon: "request", title: "Contact by request", desc: "Nobody sees your details. You send a request about a report, and the other person accepts or declines." },
    { icon: "chat", title: "Private chat", desc: "Once a request is accepted, message inside FindLoop. Chats remind you to meet in a public place and keep personal details out." },
    { icon: "shield", title: "Sign in with Google", desc: "Sign in with your Google account to report an item or contact someone, so every request comes from a signed-in person." },
  ];
  return <section className="home-section" aria-labelledby="more-title">
    <div className="home-section-head"><h2 id="more-title">Everything in one place</h2><p>Report, search, match and talk, without sharing phone numbers or IDs.</p></div>
    <div className="loop-divider" aria-hidden="true"><LoopMark /></div>
    <div className="more-grid">{items.map((f) => <article className="more-item" key={f.title}><span className="more-icon" aria-hidden="true">{featureIcons[f.icon] || moreIcons[f.icon]}</span><h3>{f.title}</h3><p>{f.desc}</p></article>)}</div>
  </section>;
}

function CallToAction({ onReport, onBrowse }) {
  return <section className="cta-band" aria-labelledby="cta-title">
    <div><h2 id="cta-title">Ready to help something find its way home?</h2><p>Report what you lost or found, and FindLoop starts looking for matches as soon as it's saved.</p></div>
    <div className="cta-actions">
      <button type="button" className="cta-btn lost" onClick={() => onReport("LOST")}>Report lost</button>
      <button type="button" className="cta-btn found" onClick={() => onReport("FOUND")}>Report found</button>
      <button type="button" className="cta-btn ghost" onClick={onBrowse}>Browse reports</button>
    </div>
  </section>;
}

function SiteFooter({ navigate }) {

  return <footer className="site-footer"><button className="footer-brand" onClick={() => navigate("home")}><span className="brand-mark"><LoopMark /></span><strong>FindLoop</strong></button><span className="footer-tagline">Lost. Found. Reunited.</span><nav aria-label="Footer navigation"><button onClick={() => navigate("home")}>Home</button><button onClick={() => navigate("browse")}>Browse</button><button onClick={() => navigate("matches")}>Matches</button></nav><LoopMark className="footer-loop" /></footer>;

}



/* ---------- Report form ---------- */

function ReportForm({ onCreated, initialType }) {

  const [f, setF] = useState({

    type: initialType || "LOST",

    title: "",

    description: "",
    category: "",
    location: "",
    locationDetails: "",
    ownershipQuestion: "",
    ownershipAnswer: "",
    latitude: "",
    longitude: "",
    eventDateTime: toLocalInput(),
    photo: "",
  });

  const [photoErr, setPhotoErr] = useState("");

  const [catPick, setCatPick] = useState("");
  const [locMsg, setLocMsg] = useState("");
  const [locBusy, setLocBusy] = useState(false);
  const [showNote, setShowNote] = useState(false);

  const [{ loading, error }, run] = useAsync();

  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });



  const pickPhoto = async (e) => {

    const file = e.target.files?.[0];

    e.target.value = "";

    if (!file) return;

    if (!file.type.startsWith("image/")) { setPhotoErr("Please choose an image file."); return; }

    try { setPhotoErr(""); setF((p) => ({ ...p, photo: "" })); const photo = await toPhoto(file); setF((p) => ({ ...p, photo })); }

    catch { setPhotoErr("That image couldn't be read. Try another one."); }

  };



  const pickCategory = (e) => {

    const v = e.target.value;

    setCatPick(v);

    setF((p) => ({ ...p, category: v === "Other" ? "" : v }));

  };

  const useMyLocation = () => {
    if (!navigator.geolocation) {
      setLocMsg("Your browser can't share its location.");
      return;
    }

    setLocBusy(true);
    setLocMsg("");
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setF((previous) => ({
          ...previous,
          latitude: pos.coords.latitude.toFixed(6),
          longitude: pos.coords.longitude.toFixed(6),
        }));
        setLocMsg(`Location added (accurate to about ${Math.round(pos.coords.accuracy)} m).`);
        setLocBusy(false);
      },
      () => {
        setLocMsg("Couldn't get your location. Allow location access and try again.");
        setLocBusy(false);
      },
      { enableHighAccuracy: true, timeout: 10000 }
    );
  };

  const toNum = (value) => (value === "" || Number.isNaN(Number(value)) ? null : Number(value));


  const submit = async (e) => {

    e.preventDefault();

    const saved = await run(() => api.createReport({

      ...f,
      eventDateTime: f.eventDateTime || null,
      photo: f.photo || null,
      locationDetails: f.locationDetails.trim() || null,
      ownershipQuestion: f.type === "FOUND" ? f.ownershipQuestion.trim() || null : null,
      ownershipAnswer: f.type === "FOUND" ? f.ownershipAnswer.trim() || null : null,
      latitude: toNum(f.latitude),
      longitude: toNum(f.longitude),

    }));

    if (saved) onCreated(saved);

  };



  return (

    <form className={`panel form spot ${f.type.toLowerCase()}`} onSubmit={submit}>
      <div className="rf-head">
        <h2>Report an item</h2>
        <Segmented id="typepill" value={f.type} onChange={(t) => setF({ ...f, type: t })}
          options={[["LOST", "I lost something"], ["FOUND", "I found something"]]} />
      </div>

      <div className="rf-grid">
        <div className="rf-col">
          <label className="field-lg">What is it?
            <input required maxLength={80} value={f.title} onChange={set("title")} placeholder="Navy blue water bottle" />
          </label>

          <div className="rf-group" role="group" aria-labelledby="cat-label">
            <span id="cat-label" className="rf-label">Category</span>
            <div className="rf-chips">
              {CATEGORIES.map((c) => (
                <button type="button" key={c} className={`rf-chip ${catPick === c ? "on" : ""}`} aria-pressed={catPick === c}
                  onClick={() => pickCategory({ target: { value: c } })}>{c}</button>
              ))}
            </div>
            <input className="sr-only" tabIndex={-1} required value={catPick} onChange={() => { }} aria-hidden="true" />
          </div>
          {catPick === "Other" && (
            <label>Type the category
              <input required maxLength={80} value={f.category} onChange={set("category")} placeholder="Umbrella, laptop, spectacles…" />
            </label>
          )}

          <label>Describe it <small>{f.description.length}/2000</small>
            <textarea required rows={4} maxLength={2000} value={f.description} onChange={set("description")}
              placeholder="Colour, brand, size, scratches, stickers." />
          </label>
        </div>

        <div className="rf-col">
          <div className="photo-field rf-photo">
            {f.photo
              ? <div className="photo-preview"><img src={f.photo} alt="Selected item" /><button type="button" onClick={() => setF({ ...f, photo: "" })} aria-label="Remove photo">×</button></div>
              : <label className="photo-drop"><input type="file" accept="image/*" onChange={pickPhoto} hidden /><span className="photo-icon" aria-hidden="true">▣</span><strong>Add a photo</strong><small>A clear picture helps people recognise it</small></label>}
            {photoErr && <p className="error" role="alert">{photoErr}</p>}
          </div>

          <label>{f.type === "LOST" ? "Where did you last see it?" : "Where did you find it?"}
            <input required maxLength={160} value={f.location} onChange={set("location")} placeholder="For example, Central Library" />
          </label>

          <div className="rf-loc">
            <button type="button" className="geo-btn" onClick={useMyLocation} disabled={locBusy}>
              <span aria-hidden="true">⌖</span> {locBusy ? "Finding you…" : "Use my location for this spot"}
            </button>
            <button type="button" className="rf-link" aria-expanded={showNote} onClick={() => setShowNote((v) => !v)}>
              {showNote ? "Hide landmark note" : "+ Add landmark note"}
            </button>
            {locMsg && <p className="rf-loc-msg" role="status">{locMsg}</p>}
            <p className="hint">Use GPS only if you are at the place where it happened. Your exact coordinates stay private.</p>
          </div>
          {showNote && (
            <label>Landmark note (optional)
              <textarea rows={2} maxLength={500} value={f.locationDetails} onChange={set("locationDetails")}
                placeholder="For example, near the east entrance or beside the information desk." />
            </label>
          )}

          <DateTimeEntry value={f.eventDateTime} onChange={(v) => setF({ ...f, eventDateTime: v })} />
          {f.type === "FOUND" && <div className="ownership-form">
            <h3>Private ownership check <span className="optional-label">Optional</span></h3>
            <p className="hint">Keep one identifying detail private. A claimant must answer before they can contact you.</p>
            <label>Question only a genuine owner could answer
              <input maxLength={240} required={!!f.ownershipAnswer.trim()} value={f.ownershipQuestion} onChange={set("ownershipQuestion")} placeholder="What sticker is on the back?" />
            </label>
            <label>Your private answer
              <input maxLength={200} required={!!f.ownershipQuestion.trim()} value={f.ownershipAnswer} onChange={set("ownershipAnswer")} placeholder="Don't include this in the public description" />
            </label>
          </div>}
        </div>
      </div>

      <Notice error={error} />

      <div className="rf-foot">
        <p className="hint public-info-note"><strong>Visible to anyone:</strong> title, category, description, place, date/time, and any landmark note. Exact GPS coordinates, your account ID, and the ownership answer stay private. Photos aren't stored or shared yet. Don't include phone numbers, home addresses, or ID numbers.</p>
        <motion.button className="primary" whileHover={{ scale: 1.02 }} whileTap={{ scale: 0.97 }} disabled={loading}>
          {loading ? "Finding matches…" : "Submit and find matches"}
        </motion.button>
      </div>
    </form>

  );

}





/* ---------- Browse ---------- */

const FactIcon = ({ children, size = 16 }) => <svg viewBox="0 0 24 24" width={size} height={size} fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{children}</svg>;

function Browse({ user, onContact }) {
  const [type, setType] = useState("");
  const [q, setQ] = useState("");
  const [category, setCategory] = useState("");
  const [near, setNear] = useState(null);
  const [radiusKm, setRadiusKm] = useState("10");
  const [geoMessage, setGeoMessage] = useState("");
  const [items, setItems] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const searchSequence = useRef(0);
  const [{ loading, error }, run] = useAsync();
  useEffect(() => {
    const timer = setTimeout(() => {
      const sequence = ++searchSequence.current;
      run(() => api.listReports(type, { q, category, ...near, radiusKm }))
        .then((d) => d && sequence === searchSequence.current && setItems(d));
    }, 250);
    return () => clearTimeout(timer);
  }, [type, q, category, near, radiusKm, run]);
  const findNearby = () => {
    if (!navigator.geolocation) { setGeoMessage("Location search isn't available in this browser."); return; }
    setGeoMessage("Getting your location…");
    navigator.geolocation.getCurrentPosition(
      ({ coords }) => { setNear({ latitude: coords.latitude, longitude: coords.longitude }); setGeoMessage("Showing reports near you."); },
      () => setGeoMessage("Couldn't get your location. Check browser permission and try again."),
      { timeout: 10000, maximumAge: 60000 }
    );
  };
  const clearNearby = () => { setNear(null); setGeoMessage(""); };
  const wide = window.matchMedia("(min-width: 901px)").matches;
  const selected = items.find((r) => r.id === selectedId) || (wide ? items[0] || null : null);
  return (
    <section className={`browse-page${selected ? " has-selected" : ""}`}>
      <div className="browse-toolbar">
        <Segmented id="filterpill" value={type} onChange={setType} options={[["", "All"], ["LOST", "Lost"], ["FOUND", "Found"]]} />
        <label className="search-wrap">
          <FactIcon size={20}><circle cx="11" cy="11" r="7" /><path d="m21 21-4.3-4.3" /></FactIcon>
          <input className="search" value={q} onChange={(e) => setQ(e.target.value)} placeholder="Search by word or place" aria-label="Search reports" />
        </label>
        <label className="browse-category">Category
          <select value={category} onChange={(e) => setCategory(e.target.value)} aria-label="Filter by category">
            <option value="">All categories</option>{CATEGORIES.filter((item) => item !== "Other").map((item) => <option key={item} value={item}>{item}</option>)}
          </select>
        </label>
        <div className="nearby-filter">
          <button type="button" className="secondary small" onClick={near ? clearNearby : findNearby}>{near ? "Clear nearby" : "Near me"}</button>
          {near && <select value={radiusKm} onChange={(e) => setRadiusKm(e.target.value)} aria-label="Search radius">
            {["1", "5", "10", "25", "50"].map((km) => <option key={km} value={km}>{km} km</option>)}
          </select>}
        </div>
      </div>
      {geoMessage && <p className="hint" role="status">{geoMessage}</p>}
      <Notice error={error} />
      {loading && <Skeleton />}
      {!loading && !error && <p className="browse-count" aria-live="polite">{items.length} {items.length === 1 ? "report" : "reports"}{q ? ` matching “${q}”` : ""}</p>}
      {!loading && !error && !items.length && <p className="inbox-empty">Nothing here yet. Submit a report to get started.</p>}
      <div className="browse-split">
        <ul className="report-list">
          {items.map((r) => (
            <li key={r.id}>
              <button type="button" className={`report-row${selected?.id === r.id ? " selected" : ""}`} aria-pressed={selected?.id === r.id} onClick={() => setSelectedId(r.id)}>
                <span className="report-row-top">
                  <span className={`tag ${r.type.toLowerCase()}`}>{r.type === "LOST" ? "Lost" : "Found"}</span>
                  <strong>{r.title}</strong>
                </span>
                <span className="report-row-meta">{r.location} · {fmt(r.eventDateTime)}</span>
              </button>
            </li>
          ))}
        </ul>
        <div className="browse-detail">
          {selected
            ? <ReportDetail key={selected.id} report={selected} user={user} onContact={onContact} onBack={() => setSelectedId(null)}
              onStatusChanged={(updated) => {
                if (updated.status === "RETURNED") {
                  setItems((current) => current.map((item) => item.id === updated.id ? { ...item, status: updated.status } : item));
                  window.setTimeout(() => setItems((current) => current.filter((item) => item.id !== updated.id)), 2600);
                } else if (updated.status !== "OPEN") setItems((current) => current.filter((item) => item.id !== updated.id));
                else setItems((current) => current.map((item) => item.id === updated.id ? { ...item, status: "OPEN" } : item));
              }} />
            : <div className="chat-placeholder"><p><strong>Select a report</strong></p><p className="hint">Its details and possible matches will appear here.</p></div>}
        </div>
      </div>
    </section>
  );
}

function ReportDetail({ report: r, user, onContact, onBack, onStatusChanged }) {
  const lost = r.type === "LOST";
  const own = r.ownedByViewer;
  const [status, setStatus] = useState(r.status);
  const [statusBusy, setStatusBusy] = useState(false);
  const [statusError, setStatusError] = useState("");
  const [reunited, setReunited] = useState(false);
  const [matchCount, setMatchCount] = useState(null);
  const changeStatus = async (nextStatus) => {
    setStatusBusy(true);
    setStatusError("");
    try {
      const updated = await api.updateReportStatus(r.id, nextStatus);
      setStatus(updated.status);
      if (nextStatus === "RETURNED") setReunited(true);
      onStatusChanged?.(updated);
    } catch (e) {
      setStatusError(e.message || "Couldn't update this report.");
    } finally {
      setStatusBusy(false);
    }
  };
  return (
    <article className="detail">
      <button type="button" className="secondary small detail-back" onClick={onBack}>← All reports</button>
      {r.photo
        ? <img className="detail-photo" src={r.photo} alt="" />
        : <div className={`detail-photo placeholder ${lost ? "lost" : "found"}`} aria-hidden="true"><FactIcon size={44}><path d="M6 8h12l1 12H5z" /><path d="M9 8V6a3 3 0 0 1 6 0v2" /></FactIcon></div>}
      <div className="item-top">
        <span className={`tag ${lost ? "lost" : "found"}`}>{lost ? "Lost" : "Found"}</span>
        {status !== "OPEN" && <span className={`status-chip status-${status.toLowerCase()}`}>{status === "RETURNED" ? "Returned" : "Closed"}</span>}
        <span className="item-cat">{r.category}</span>
        <span className="item-cat">Report #{r.id}</span>
      </div>
      {reunited && status === "RETURNED" && <ReunitedCelebration />}
      <div className="detail-title-row">
        <h2>{r.title}</h2>
        <button type="button" className="primary" onClick={() => onContact?.(r.id, null)} disabled={!user || own || status !== "OPEN"}
          title={!user ? "Sign in to contact the report owner" : own ? "This is your own report" : status !== "OPEN" ? "This report is no longer open" : "Send a private contact request"}>
          {!user ? "Sign in to contact" : own ? "Your report" : status !== "OPEN" ? "Report closed" : "Contact owner"}
        </button>
      </div>
      <dl className="detail-facts">
        <div><dt>Where</dt><dd>{r.location}</dd></div>
        {r.locationDetails && <div><dt>Location details</dt><dd>{r.locationDetails}</dd></div>}
        <div><dt>{lost ? "Lost on" : "Found on"}</dt><dd>{fmt(r.eventDateTime)}</dd></div>
        <div><dt>Category</dt><dd>{r.category}</dd></div>
      </dl>
      <h3>Description</h3>
      <p className="detail-desc">{r.description}</p>
      {own && <div className="report-lifecycle">
        <p className="hint">{status === "OPEN" ? "Still looking? Close this report when the item is returned or you no longer need help." : "This report is hidden from open search and match results."}</p>
        <div className="report-lifecycle-actions">
          {status === "OPEN" ? <>
            <button type="button" className="secondary small" disabled={statusBusy} onClick={() => changeStatus("RETURNED")}>Mark as returned</button>
            <button type="button" className="secondary small" disabled={statusBusy} onClick={() => changeStatus("CLOSED")}>Close report</button>
          </> : <button type="button" className="secondary small" disabled={statusBusy} onClick={() => changeStatus("OPEN")}>Reopen report</button>}
        </div>
        {statusError && <p role="alert" className="error">{statusError}</p>}
      </div>}
      <section className="detail-matches" aria-label="Possible matches">
        <div className="detail-matches-head">
          <div><h3>Possible matches</h3><p className="hint">Comparing with open {lost ? "found" : "lost"} reports.</p></div>
          {matchCount > 0 && <span className="detail-match-count">{matchCount} {matchCount === 1 ? "possible match" : "possible matches"}</span>}
        </div>
        <MatchResults key={r.id} report={r} user={user} onContact={onContact} onCountChange={setMatchCount} />
      </section>
    </article>
  );
}

const Bar = ({ label, value }) => (

  <div className="bar" title={`${label}: ${value}%`}>

    <span>{label}</span>

    <div className="track"><motion.i initial={{ width: 0 }} animate={{ width: `${value}%` }} transition={{ duration: 0.9, ease: "easeOut" }} /></div>

    <b>{Math.round(value)}</b>

  </div>

);



function Ring({ score }) {

  const r = 28, c = 2 * Math.PI * r;

  return (

    <div className="ring">

      <svg viewBox="0 0 72 72">

        <circle cx="36" cy="36" r={r} className="bg" />

        <motion.circle cx="36" cy="36" r={r} className="fg" strokeDasharray={c} initial={{ strokeDashoffset: c }}

          animate={{ strokeDashoffset: c * (1 - score / 100) }} transition={{ duration: 1.1, ease: "easeOut" }} />

      </svg>

      <span><CountUp value={score} /></span>

    </div>

  );

}



function MatchResults({ report, user, onContact, onCountChange }) {
  const [matches, setMatches] = useState([]);

  const [details, setDetails] = useState({});

  const [{ loading, error }, run] = useAsync();
  const [feedbackError, setFeedbackError] = useState("");



  useEffect(() => {

    if (!report) return;

    run(async () => {

      const found = await api.getMatches(report.id);

      setMatches(found);
      onCountChange?.(found.length);

      const full = await Promise.all(found.map((m) => api.getReport(m.candidateId).catch(() => null)));

      setDetails(Object.fromEntries(full.filter(Boolean).map((x) => [x.id, x])));

    });

  }, [report, run, onCountChange]);
  return (
    <>
      <Notice error={error} />

      {loading && !matches.length && <><Skeleton /><Skeleton /></>}

      {!loading && !error && !matches.length && <EmptyIllustration kind="matches" title="No matches yet" text="New reports are compared as they arrive. We’ll show possible matches here." />}

      <motion.ul className="list match-list" variants={list} initial="hidden" animate="show">

        {matches.map((m, i) => {

          const d = details[m.candidateId];

          return (

            <motion.li key={m.candidateId} variants={rise} className={`match spot ${i === 0 ? "best" : ""}`}>

              <Ring score={m.finalScore} />

              <div className="body">

                {i === 0 && <motion.span className="badge" initial={{ scale: 0 }} animate={{ scale: 1 }} transition={{ delay: 0.5, type: "spring" }}>Best match</motion.span>}

                <strong>{m.candidateTitle}</strong>

                {d && (
                  <ul className="item-facts">
                    <li><FactIcon><path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z" /><circle cx="12" cy="10" r="3" /></FactIcon>{d.location}</li>
                    <li><FactIcon><circle cx="12" cy="12" r="9.5" /><path d="M12 6.5V12l3.8 2.2" /></FactIcon>{fmt(d.eventDateTime)}</li>
                    <li><FactIcon><path d="M20.6 13.4 13.4 20.6a2 2 0 0 1-2.8 0L3 13V3h10l7.6 7.6a2 2 0 0 1 0 2.8z" /><circle cx="7.5" cy="7.5" r="1.5" /></FactIcon>{d.category}</li>
                  </ul>
                )}

                {d && <p>{d.description}</p>}

                <Bar label="Description" value={m.descriptionScore} />

                <Bar label="Location" value={m.locationScore} />

                <Bar label="Time" value={m.dateTimeScore} />

                <ul className="chips">{chips(m).map((t) => <li key={t}>{t}</li>)}</ul>

                <section className="match-evidence" aria-label="Why this match">
                  <strong>Why we suggested this</strong>
                  <ul>
                    {m.sharedDetails?.length > 0 && <li>Both reports mention {m.sharedDetails.map((detail) => `“${detail}”`).join(", ")}</li>}
                    {m.distanceKm != null && <li>Reported locations are about {m.distanceKm < 1 ? `${Math.max(1, Math.round(m.distanceKm * 1000))} metres` : `${m.distanceKm.toFixed(1)} km`} apart in a straight line</li>}
                    {m.distanceKm == null && m.sameLocationText && report.location && <li>Both reports list {report.location}</li>}
                    {m.timeGapMinutes != null && <li>The events were about {formatTimeGap(m.timeGapMinutes)} apart</li>}
                    {!m.sharedDetails?.length && m.distanceKm == null && !m.sameLocationText && m.timeGapMinutes == null && <li>We could not identify specific shared details.</li>}
                  </ul>
                </section>

                <p className="why">Similarity is not proof of ownership. Verify before handing anything over.</p>
                {user && <button type="button" className="not-match-button" onClick={async () => {
                  setFeedbackError("");
                  try { await api.notAMatch(report.id, m.candidateId); const remaining = matches.filter((item) => item.candidateId !== m.candidateId); setMatches(remaining); onCountChange?.(remaining.length); }
                  catch (e) { setFeedbackError(e.message || "Couldn't save that feedback."); }
                }}>Not a match</button>}

              </div>
              {d?.photo && <img className="thumb" src={d.photo} alt="" />}
              {d && (
                <button type="button" className="primary"
                  onClick={() => onContact?.(m.candidateId, report.id)}
                  disabled={!user || d.ownedByViewer}
                  title={!user ? "Sign in to contact the report owner" : d.ownedByViewer ? "This is your own report" : "Send a private contact request"}>
                  {!user ? "Sign in to contact" : d.ownedByViewer ? "Your report" : "Contact owner"}
                </button>
              )}
            </motion.li>

          );

        })}

      </motion.ul>
      {feedbackError && <p className="error" role="alert">{feedbackError}</p>}
    </>
  );
}

function EmptyIllustration({ kind, title, text }) {
  return <div className={`empty-illustration empty-${kind}`} role="status">
    <span className="empty-art" aria-hidden="true"><FactIcon size={34}>{kind === "matches"
      ? <><circle cx="10.5" cy="10.5" r="6.5" /><path d="m16 16 5 5M7.5 10.5h6M10.5 7.5v6" /></>
      : <><path d="M4 5h16v12H8l-4 3z" /><path d="M8 9h8M8 13h5" /></>}</FactIcon></span>
    <strong>{title}</strong><span>{text}</span>
  </div>;
}

function ReunitedCelebration() {
  return <motion.div className="reunited-celebration" role="status" initial={{ opacity: 0, y: 8, scale: .96 }} animate={{ opacity: 1, y: 0, scale: 1 }}>
    <span aria-hidden="true">✦</span><strong>Reunited!</strong><span>Glad this item found its way home.</span><span aria-hidden="true">✦</span>
  </motion.div>;
}

function Matches({ report, user, onContact }) {
  const [status, setStatus] = useState(report?.status || "OPEN");
  const [statusBusy, setStatusBusy] = useState(false);
  const [statusError, setStatusError] = useState("");
  const [reunited, setReunited] = useState(false);
  if (!report) return <section className="panel spot"><p className="hint">Submit a report or pick one from Browse to see its matches.</p></section>;
  const updateStatus = async (next) => {
    setStatusBusy(true); setStatusError("");
    try { const updated = await api.updateReportStatus(report.id, next); setStatus(updated.status); if (next === "RETURNED") setReunited(true); }
    catch (e) { setStatusError(e.message || "Couldn't update this report."); }
    finally { setStatusBusy(false); }
  };
  return (
    <section className="panel spot">
      <h2>Matches for “{report.title}”</h2>
      {reunited && status === "RETURNED" && <ReunitedCelebration />}
      {report.ownedByViewer && <div className="report-lifecycle">
        {status === "OPEN" ? <>
          <p className="hint">Got it back? Mark this report returned so it disappears from open search and matches.</p>
          <div className="report-lifecycle-actions"><button type="button" className="secondary small" disabled={statusBusy} onClick={() => updateStatus("RETURNED")}>Mark as returned</button><button type="button" className="secondary small" disabled={statusBusy} onClick={() => updateStatus("CLOSED")}>Close report</button></div>
        </> : <><p className="hint">This report is {status === "RETURNED" ? "marked returned" : "closed"} and hidden from open search and matches.</p><button type="button" className="secondary small" disabled={statusBusy} onClick={() => updateStatus("OPEN")}>Reopen report</button></>}
        {statusError && <p className="error" role="alert">{statusError}</p>}
      </div>}
      <p className="hint">Comparing with open {report.type === "LOST" ? "found" : "lost"} reports.</p>
      {status === "OPEN" ? <MatchResults key={report.id} report={report} user={user} onContact={onContact} /> : <p className="hint">No active match results for a resolved report.</p>}
    </section>
  );
}

/* ---------- Messages ---------- */

function Messages({ user, onToast, initialConversationId }) {

  const [received, setReceived] = useState([]);

  const [sent, setSent] = useState([]);

  const [conversations, setConversations] = useState([]);

  const [active, setActive] = useState(null);

  const [messages, setMessages] = useState([]);

  const [draft, setDraft] = useState("");

  const [loading, setLoading] = useState(true);

  const [error, setError] = useState("");
  const historyRef = useRef(null);
  useEffect(() => {
    const el = historyRef.current;
    if (el) el.scrollTop = el.scrollHeight;
  }, [messages, active]);


  useEffect(() => {
    if (!active?.id) return;

    const conversationId = active.id;
    let cancelled = false;
    let refreshing = false;
    let refreshAgain = false;

    const refreshMessages = async () => {
      if (refreshing) {
        refreshAgain = true;
        return;
      }

      refreshing = true;

      try {
        do {
          refreshAgain = false;

          const data = await api.getMessages(conversationId);

          if (!cancelled) {
            setMessages(data || []);
          }
        } while (refreshAgain && !cancelled);
      } catch {
        // Keep the current messages if a refresh fails.
      } finally {
        refreshing = false;
      }
    };

    const channel = supabase
      .channel(`chat-messages-${conversationId}`)
      .on(
        "postgres_changes",
        {
          event: "INSERT",
          schema: "public",
          table: "chat_messages",
          filter: `conversation_id=eq.${conversationId}`,
        },
        () => {
          void refreshMessages();
        }
      )
      .subscribe((status) => {
        if (status === "SUBSCRIBED") {
          void refreshMessages();
        }
      });

    return () => {
      cancelled = true;
      void supabase.removeChannel(channel);
    };
  }, [active?.id]);


  const refresh = async () => {

    setLoading(true);

    setError("");



    try {

      const [incoming, outgoing, chats] = await Promise.all([

        api.getReceivedRequests(),

        api.getSentRequests(),

        api.getConversations(),

      ]);



      setReceived(incoming || []);

      setSent(outgoing || []);

      setConversations(chats || []);
      const initialChat = (chats || []).find((chat) => chat.id === initialConversationId);
      if (initialChat) {
        setActive(initialChat);
        setMessages(await api.getMessages(initialChat.id));
      }

    } catch (e) {

      setError(e.message || "Couldn't load messages.");

    } finally {

      setLoading(false);

    }

  };



  useEffect(() => {
    if (!user) return undefined;
    let activeRequest = true;
    const loadInbox = async () => {
      try {
        const [incoming, outgoing, chats] = await Promise.all([
          api.getReceivedRequests(), api.getSentRequests(), api.getConversations(),
        ]);
        if (!activeRequest) return;
        setReceived(incoming || []);
        setSent(outgoing || []);
        setConversations(chats || []);
        const initialChat = (chats || []).find((chat) => chat.id === initialConversationId);
        if (initialChat) {
          setActive(initialChat);
          const initialMessages = await api.getMessages(initialChat.id);
          if (activeRequest) setMessages(initialMessages || []);
        }
      } catch (e) {
        if (activeRequest) setError(e.message || "Couldn't load messages.");
      } finally {
        if (activeRequest) setLoading(false);
      }
    };
    void loadInbox();
    return () => { activeRequest = false; };
  }, [user, initialConversationId]);



  const respond = async (requestId, decision) => {

    try {

      await api.respondToRequest(requestId, decision);



      if (decision === "ACCEPTED") {

        const chat = await api.openConversation(requestId);

        setActive(chat);

        setMessages([]);

      }



      await refresh();

      onToast(decision === "ACCEPTED" ? "Request accepted." : "Request declined.");

    } catch (e) {

      setError(e.message || "Couldn't update the request.");

    }

  };



  const openChat = async (requestId) => {

    try {

      const chat = await api.openConversation(requestId);

      setActive(chat);

      const data = await api.getMessages(chat.id);

      setMessages(data || []);

    } catch (e) {

      setError(e.message || "Couldn't open this chat.");

    }

  };



  const send = async (e) => {

    e.preventDefault();

    if (!active || !draft.trim()) return;



    try {

      const saved = await api.sendMessage(active.id, draft.trim());

      setMessages((previous) => [...previous, saved]);

      setDraft("");

    } catch (e) {

      setError(e.message || "Couldn't send your message.");

    }

  };



  if (!user) {

    return (

      <section className="panel spot">

        <h2>Sign in to view messages</h2>

        <p className="hint">Your contact requests and private chats are available after login.</p>

      </section>

    );

  }



  const statusLabel = (s) => (s ? s.charAt(0) + s.slice(1).toLowerCase() : "");
  const chip = (s) => <span className={`status-chip status-${(s || "").toLowerCase()}`}>{statusLabel(s)}</span>;

  return (
    <section className={`panel spot messages-page${active ? " has-active" : ""}`}>
      <div className="messages-heading">
        <div>
          <h2>Your messages</h2>
          <p className="hint">Contact requests and private conversations.</p>
        </div>
        <button type="button" className="outline-button" onClick={refresh} disabled={loading}>
          {loading ? "Refreshing…" : "Refresh"}
        </button>
      </div>
      {error && <Notice error={error} />}

      <div className="inbox">
        <aside className="inbox-list" aria-label="Requests and conversations">
          <section className="inbox-group">
            <h3>Requests received</h3>
            {!received.length && <p className="inbox-empty">No incoming requests right now.</p>}
            {received.map((r) => (
              <article className="inbox-item" key={r.id}>
                <div className="inbox-item-text">
                  <strong>Report #{r.reportId}</strong>
                  {chip(r.status)}
                </div>
                {r.status === "PENDING" && (
                  <div className="request-actions">
                    <button type="button" className="primary small" onClick={() => respond(r.id, "ACCEPTED")}>Accept</button>
                    <button type="button" className="secondary small" onClick={() => respond(r.id, "DECLINED")}>Decline</button>
                  </div>
                )}
                {r.status === "ACCEPTED" && (
                  <button type="button" className="primary small" onClick={() => openChat(r.id)}>Chat</button>
                )}
              </article>
            ))}
          </section>

          <section className="inbox-group">
            <h3>Requests sent</h3>
            {!sent.length && <p className="inbox-empty">You haven't sent any contact requests yet.</p>}
            {sent.map((r) => (
              <article className="inbox-item" key={r.id}>
                <div className="inbox-item-text">
                  <strong>Report #{r.reportId}</strong>
                  {chip(r.status)}
                </div>
                {r.status === "ACCEPTED" && (
                  <button type="button" className="primary small" onClick={() => openChat(r.id)}>Chat</button>
                )}
              </article>
            ))}
          </section>

          <section className="inbox-group">
            <h3>Your conversations</h3>
            {!conversations.length && <p className="inbox-empty">Accepted conversations will appear here.</p>}
            {conversations.map((chat) => (
              <article className={`inbox-item${active?.id === chat.id ? " selected" : ""}`} key={chat.id}>
                <div className="inbox-item-text">
                  <strong>Conversation #{chat.id}</strong>
                  <span className="meta">Report-linked private chat</span>
                </div>
                <button
                  type="button"
                  className="primary small"
                  aria-current={active?.id === chat.id ? "true" : undefined}
                  onClick={async () => {
                    try {
                      setActive(chat);
                      setMessages(await api.getMessages(chat.id));
                    } catch (e) {
                      setError(e.message || "Couldn't load this conversation.");
                    }
                  }}
                >
                  Open chat
                </button>
              </article>
            ))}
          </section>
        </aside>

        <div className="chat-pane">
          {active ? (
            <div className="chat-panel">
              <div className="chat-header">
                <div>
                  <h3>Private chat</h3>
                  <small>Conversation #{active.id}</small>
                </div>
                <button type="button" className="secondary small" onClick={() => setActive(null)}>Close</button>
              </div>
              <p className="chat-safety">
                <svg aria-hidden="true" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6z" /></svg>
                <span>
                  Arrange handover at a safe public place, such as a college security office.
                  Don't share phone numbers or sensitive personal information here.
                </span>
              </p>
              <div className="chat-history" aria-live="polite" ref={historyRef}>
                {!messages.length && <EmptyIllustration kind="messages" title="No messages yet" text="Start the conversation when you’re ready." />}
                {messages.map((m) => (
                  <div
                    key={m.id}
                    className={`chat-bubble ${m.senderId === user.id ? "mine" : ""}`}
                  >
                    <p>{m.content}</p>
                    <small>{m.sentAt ? fmt(m.sentAt) : ""}</small>
                  </div>
                ))}
              </div>
              <form className="chat-compose" onSubmit={send}>
                <input
                  value={draft}
                  onChange={(e) => setDraft(e.target.value)}
                  maxLength={2000}
                  placeholder="Write a message…"
                  aria-label="Message"
                  required
                />
                <button className="primary" disabled={!draft.trim()}>Send</button>
              </form>
            </div>
          ) : (
            <div className="chat-placeholder">
              <p><strong>Select a conversation</strong></p>
              <p className="hint">Accepted requests appear under "Your conversations".</p>
            </div>
          )}
        </div>
      </div>
    </section>
  );
}

function NotificationFrequencySelect({ label, value, disabled, onChange }) {
  return <label className="notification-setting">{label}
    <select value={value} disabled={disabled} onChange={(e) => onChange(e.target.value)}>
      <option value="OFF">Off</option><option value="IMMEDIATELY">Immediately</option><option value="DAILY">Daily roundup</option>
    </select>
  </label>;
}

function SettingsPage({ user, onOpenNotification }) {
  const [section, setSection] = useState("notifications");
  const [preferences, setPreferences] = useState({ matchFrequency: "OFF", replyFrequency: "OFF" });
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!user) return undefined;
    let active = true;
    const refresh = async () => {
      try {
        const data = await api.getNotifications();
        if (active) setNotifications(data || []);
      } catch (e) { if (active) setError(e.message || "Couldn't load notifications."); }
    };
    Promise.all([api.getNotificationPreferences(), api.getNotifications()])
      .then(([prefs, items]) => {
        if (active) { setPreferences(prefs); setNotifications(items || []); setError(""); }
      })
      .catch((e) => { if (active) setError(e.message || "Couldn't load notifications."); })
      .finally(() => { if (active) setLoading(false); });
    const frequencies = [preferences.matchFrequency, preferences.replyFrequency];
    const interval = frequencies.includes("IMMEDIATELY") ? 60_000
      : frequencies.includes("DAILY") ? 60 * 60_000 : null;
    const timer = interval ? window.setInterval(refresh, interval) : null;
    return () => { active = false; if (timer) window.clearInterval(timer); };
  }, [user, preferences.matchFrequency, preferences.replyFrequency]);

  const updateFrequency = async (key, value) => {
    const next = { ...preferences, [key]: value };
    setSaving(true); setError("");
    try { setPreferences(await api.updateNotificationPreferences(next)); }
    catch (e) { setError(e.message || "Couldn't save notification preferences."); }
    finally { setSaving(false); }
  };

  const openNotification = async (notification) => {
    try {
      await api.markNotificationRead(notification.id);
      setNotifications((current) => current.filter((item) => item.id !== notification.id));
      onOpenNotification(notification);
    } catch (e) { setError(e.message || "Couldn't open this notification."); }
  };

  if (!user) return <section className="panel spot"><h2>Sign in to manage settings</h2><p className="hint">Choose whether FindLoop should notify you about possible matches and private replies.</p></section>;

  return <div className="settings-layout">
    <nav className="settings-sections" aria-label="Settings sections">
      <span className="settings-section-label">YOUR SETTINGS</span>
      <button type="button" className={`settings-section ${section === "notifications" ? "active" : ""}`} aria-current={section === "notifications" ? "page" : undefined} onClick={() => setSection("notifications")}><span aria-hidden="true">♢</span> Notifications</button>
      <button type="button" className={`settings-section ${section === "privacy" ? "active" : ""}`} aria-current={section === "privacy" ? "page" : undefined} onClick={() => setSection("privacy")}><span aria-hidden="true">⌑</span> Privacy</button>
    </nav>
    {section === "notifications" ? <section className="panel settings-panel notification-center" aria-labelledby="settings-notifications-title">
      <div className="settings-panel-head">
        <h2 id="settings-notifications-title">Notifications</h2>
        <p className="hint">Choose which updates you receive and how often.</p>
      </div>
      <div className="settings-group">
        <h3>Notification preferences</h3>
        <p className="hint">Daily roundup groups updates together after 24 hours. Email and push notifications aren't enabled.</p>
        <div className="notification-settings">
          <NotificationFrequencySelect label="Possible match suggestions" value={preferences.matchFrequency} disabled={saving} onChange={(value) => updateFrequency("matchFrequency", value)} />
          <NotificationFrequencySelect label="Private message replies" value={preferences.replyFrequency} disabled={saving} onChange={(value) => updateFrequency("replyFrequency", value)} />
        </div>
      </div>
      {error && <p className="error" role="alert">{error}</p>}
      <div className="settings-group settings-inbox">
        <h3>Unread notifications</h3>
        {loading && <Skeleton />}
        {!loading && !notifications.length && <p className="hint">No unread notifications. Turn on an update above to hear about future activity.</p>}
        <ul className="notification-list">
          {notifications.map((item) => <li key={item.id}>
            <button type="button" onClick={() => openNotification(item)}>
              <strong>{item.title}</strong><span>{item.message}</span><small>{fmt(item.createdAt)} · {item.kind === "MATCH" ? "Possible match" : "Private reply"}</small>
            </button>
          </li>)}
        </ul>
      </div>
    </section> : <section className="panel settings-panel settings-privacy" aria-labelledby="settings-privacy-title">
      <div className="settings-panel-head">
        <h2 id="settings-privacy-title">What others can see</h2>
        <p className="hint">Your reports help people identify an item while keeping account details private.</p>
      </div>
      <div className="settings-group">
        <h3>Public on your reports</h3>
        <p className="hint">Title, category, description, place, date and time, landmark notes, and any photo you attach.</p>
      </div>
      <div className="settings-group">
        <h3>Kept private</h3>
        <p className="hint">Your account ID, exact GPS coordinates, email, and private conversations aren't shown on public reports.</p>
      </div>
      <div className="settings-group">
        <p className="hint">Avoid including phone numbers, home addresses, or ID numbers in a report description or landmark note.</p>
      </div>
    </section>}
  </div>;
}

/* ---------- App shell ---------- */

const initialsOf = (u) => {
  const n = (u?.user_metadata?.full_name || u?.email || "?").trim();
  const parts = n.split(/[\s@._-]+/).filter(Boolean);
  return ((parts[0]?.[0] || "?") + (parts[1]?.[0] || "")).toUpperCase();
};

const GoogleG = () => (
  <svg viewBox="0 0 48 48" width="18" height="18" aria-hidden="true">
    <path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z" />
    <path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z" />
    <path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z" />
    <path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z" />
  </svg>
);

function AuthArea({ user, onSignIn, onSignOut, onSettings }) {
  const [open, setOpen] = useState(false);
  const [busy, setBusy] = useState(false);
  const ref = useRef(null);
  useEffect(() => {
    if (!open) return undefined;
    const onDown = (e) => { if (ref.current && !ref.current.contains(e.target)) setOpen(false); };
    const onKey = (e) => { if (e.key === "Escape") setOpen(false); };
    document.addEventListener("mousedown", onDown);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("mousedown", onDown);
      document.removeEventListener("keydown", onKey);
    };
  }, [open]);

  if (!user) {
    return (
      <button type="button" className="auth-btn auth-signin" onClick={onSignIn}>
        <span className="auth-g"><GoogleG /></span>
        Sign in with Google
      </button>
    );
  }
  const name = user.user_metadata?.full_name || user.email;
  const photo = user.user_metadata?.avatar_url || user.user_metadata?.picture;
  const face = photo
    ? <img src={photo} alt="" referrerPolicy="no-referrer" />
    : initialsOf(user);
  return (
    <div className="user-menu" ref={ref}>
      <button
        type="button"
        className="user-avatar-btn"
        aria-haspopup="menu"
        aria-expanded={open}
        aria-label={`Account menu for ${name}`}
        onClick={() => setOpen((o) => !o)}
      >
        <span className="user-avatar" aria-hidden="true">{face}</span>
      </button>
      {open && (
        <div className="account-menu" role="menu">
          <div className="account-head">
            <span className="user-avatar big" aria-hidden="true">{face}</span>
            <span className="user-meta">
              <span className="user-name">{name}</span>
              {name !== user.email && <span className="user-sub">{user.email}</span>}
            </span>
          </div>
          <button
            type="button"
            role="menuitem"
            className="account-settings"
            autoFocus
            onClick={() => { setOpen(false); onSettings(); }}
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><circle cx="12" cy="12" r="3" /><path d="M19.4 15a1.7 1.7 0 0 0 .3 1.9l.1.1-1.5 2.6-.2-.1a1.7 1.7 0 0 0-1.9.3l-.1.1h-3l-.1-.2a1.7 1.7 0 0 0-1.5-1l-.2.1-2.6-1.5.1-.2a1.7 1.7 0 0 0-.3-1.9l-.1-.1v-3l.2-.1a1.7 1.7 0 0 0 1-1.5l-.1-.2 1.5-2.6.2.1a1.7 1.7 0 0 0 1.9-.3l.1-.1h3l.1.2a1.7 1.7 0 0 0 1.5 1l.2-.1 2.6 1.5-.1.2a1.7 1.7 0 0 0 .3 1.9l.1.1v3z" /></svg>
            Settings
          </button>
          <button
            type="button"
            role="menuitem"
            className="account-logout"
            disabled={busy}
            onClick={async () => {
              setBusy(true);
              try { await onSignOut(); setOpen(false); } finally { setBusy(false); }
            }}
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" /><path d="m16 17 5-5-5-5" /><path d="M21 12H9" /></svg>
            Log out
          </button>
        </div>
      )}
    </div>
  );
}

export default function App() {

  const [tab, setTab] = useState("home");

  const [current, setCurrent] = useState(null);

  const [toast, setToast] = useState("");

  const [reportType, setReportType] = useState("LOST");

  const [user, setUser] = useState(null);
  const [pendingConversationId, setPendingConversationId] = useState(null);
  const signInWithGoogle = async () => {

    const { error } = await supabase.auth.signInWithOAuth({
      provider: "google",
      options: {
        redirectTo: window.location.origin,
        queryParams: {
          prompt: "select_account",
        },
      },
    });


    if (error) {
      setToast(`Login failed: ${error.message}`);
      setTimeout(() => setToast(""), 3000);
    }
  };

  useEffect(() => {

    supabase.auth.getSession().then(({ data }) => {

      setUser(data.session?.user ?? null);

    });

    const {

      data: { subscription },

    } = supabase.auth.onAuthStateChange((_event, session) => {

      setUser(session?.user ?? null);

    });

    return () => subscription.unsubscribe();

  }, []);

  const go = (r, created) => {

    setCurrent(r); setTab("matches");

    if (created) { setToast("Report saved. Looking for matches…"); setTimeout(() => setToast(""), 2800); }
  };

  const openReport = (type) => {
    if (!user) {
      signInWithGoogle();
      return;
    }
    setReportType(type);

    setTab("report");

    window.scrollTo({ top: 0, behavior: "smooth" });

  };
  const navigate = (nextTab) => { setTab(nextTab); window.scrollTo({ top: 0, behavior: "smooth" }); };

  const openNotification = async (notification) => {
    if (notification.kind === "MATCH") {
      try {
        const report = await api.getReport(notification.referenceId);
        setCurrent(report);
        navigate("matches");
      } catch (e) { setToast(e.message || "Couldn't open the matching report."); }
      return;
    }
    setPendingConversationId(notification.referenceId);
    navigate("messages");
  };

  const [contactTarget, setContactTarget] = useState(null);
  const [contactMessage, setContactMessage] = useState("");
  const [ownershipChallenge, setOwnershipChallenge] = useState(null);
  const [ownershipAnswer, setOwnershipAnswer] = useState("");
  const [ownershipError, setOwnershipError] = useState("");
  const [ownershipBusy, setOwnershipBusy] = useState(false);
  const contactOwner = async (candidateReportId, sourceReportId) => {
    if (!user) {
      await signInWithGoogle();
      return;
    }

    try {
      const candidate = await api.getReport(candidateReportId);
      const source = sourceReportId
        ? await api.getReport(sourceReportId).catch(() => null)
        : null;

      const foundReport =
        candidate.type === "FOUND"
          ? candidate
          : source?.type === "FOUND"
            ? source
            : null;

      const ownershipReportId = foundReport?.id ?? null;

      const { question } = ownershipReportId
        ? await api.getOwnershipQuestion(ownershipReportId)
        : { question: null };

      const target = {
        reportId:
          candidate.type === "FOUND" && source?.type === "LOST"
            ? source.id
            : candidateReportId,
        sourceReportId,
        ownershipReportId: question ? ownershipReportId : null,
      };

      if (question) {
        setOwnershipChallenge({ ...target, question });
        setOwnershipAnswer("");
        setOwnershipError("");
      } else {
        openContactDialog(target);
      }
    } catch (e) {
      setToast(e.message || "Couldn't open the ownership check.");
      setTimeout(() => setToast(""), 3000);
    }
  };

  const openContactDialog = (target) => { setContactTarget(target); setContactMessage(""); };

  return <MotionConfig reducedMotion="user">

    <div className="app-shell">

      <header className="site-header">

        <button className="brand" onClick={() => navigate("home")} aria-label="FindLoop home"><span className="brand-mark"><LoopMark /></span><span className="brand-stack"><span className="brand-name">FindLoop</span></span></button>

        <nav className="site-nav" aria-label="Main navigation">

          <button className={tab === "home" ? "active" : ""} onClick={() => navigate("home")}><span>⌂</span> Home</button>

          <button className={tab === "browse" ? "active" : ""} onClick={() => navigate("browse")}><span>↗</span> Browse</button>

          <button

            className={tab === "messages" ? "active" : ""}

            onClick={() => {

              if (!user) {

                signInWithGoogle();

                return;

              }

              navigate("messages");

            }}

          >

            <span>✉</span> Messages

          </button>

        </nav>

        <AuthArea user={user} onSignIn={signInWithGoogle} onSignOut={() => supabase.auth.signOut()} onSettings={() => navigate("settings")} />
      </header>

      {tab === "home" && <>

        <Hero onReport={openReport} />

        <FeatureHighlights />

        <HowItWorks />

        <MoreFeatures />

        <CallToAction onReport={openReport} onBrowse={() => navigate("browse")} />

      </>}
      {tab !== "home" && (
        <AnimatePresence mode="wait">
          <motion.main
            className={`app-main app-main-${tab}`}
            key="subpage"
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -8 }}
            transition={{ duration: 0.2 }}
          >
            <div className="subpage-heading">
              <h1>
                {tab === "report"
                  ? (reportType === "LOST" ? "Report a lost item" : "Report a found item")
                  : tab === "browse"
                    ? "Browse reports"
                    : tab === "matches"
                      ? "Possible matches"
                      : tab === "settings"
                        ? "Settings"
                        : "Messages"}
              </h1>
              <button
                type="button"
                className="outline-button"
                onClick={() => navigate("home")}
              >
                ← Back to home
              </button>
            </div>

            {tab === "report" && (
              <ReportForm
                key={reportType}
                initialType={reportType}
                onCreated={(r) => go(r, true)}
              />
            )}

            {tab === "browse" && <Browse user={user} onContact={contactOwner} />}

            {tab === "matches" && (
              <Matches
                key={current?.id || "empty"}
                report={current}
                user={user}
                onContact={contactOwner}
              />
            )}

            {tab === "messages" && (
              <Messages
                user={user}
                initialConversationId={pendingConversationId}
                onToast={(msg) => {
                  setToast(msg);
                  setTimeout(() => setToast(""), 2800);
                }}
              />
            )}

            {tab === "settings" && (
              <SettingsPage
                user={user}
                onOpenNotification={openNotification}
              />
            )}
          </motion.main>
        </AnimatePresence>
      )}

      {contactTarget && (
        <div className="contact-overlay" onClick={() => setContactTarget(null)}>
          <section
            className="contact-dialog"
            role="dialog"
            aria-modal="true"
            aria-labelledby="contact-dialog-title"
            onClick={(e) => e.stopPropagation()}
          >
            <button
              type="button"
              className="contact-close"
              onClick={() => setContactTarget(null)}
              aria-label="Close contact window"
            >
              ×
            </button>

            <p className="eyebrow">PRIVATE CONTACT</p>
            <h2 id="contact-dialog-title">Start a conversation</h2>
            <p>
              Send a contact request about this report. Once the other person
              accepts, you can chat privately through Messages.
            </p>

            <label className="contact-label" htmlFor="contact-message">
              Optional introduction
            </label>
            <textarea
              id="contact-message"
              value={contactMessage}
              onChange={(e) => setContactMessage(e.target.value)}
              maxLength={500}
              rows={4}
              placeholder="Explain why you think this report matches your item…"
            />

            <p className="hint">
              Don't include phone numbers, passwords, or sensitive personal information.
            </p>

            <div className="contact-actions">
              <button
                type="button"
                className="secondary"
                onClick={() => setContactTarget(null)}
              >
                Cancel
              </button>
              <button
                type="button"
                className="primary"
                onClick={async () => {
                  try {
                    await api.sendContactRequest(contactTarget.reportId, contactTarget.ownershipReportId);

                    setContactTarget(null);
                    setContactMessage("");
                    setToast("Contact request sent! Track it in Messages.");
                    setTimeout(() => setToast(""), 3000);
                  } catch (e) {
                    setToast(e.message || "Couldn't send the contact request.");
                    setTimeout(() => setToast(""), 3000);
                  }
                }}
              >
                Send Contact Request
              </button>
            </div>
          </section>
        </div>
      )}

      {ownershipChallenge && <div className="contact-overlay" onClick={() => setOwnershipChallenge(null)}>
        <form className="contact-dialog ownership-dialog" role="dialog" aria-modal="true" aria-labelledby="ownership-dialog-title" onClick={(e) => e.stopPropagation()} onSubmit={async (e) => {
          e.preventDefault(); setOwnershipBusy(true); setOwnershipError("");
          try { await api.verifyOwnershipAnswer(ownershipChallenge.ownershipReportId, ownershipAnswer); const target = ownershipChallenge; setOwnershipChallenge(null); openContactDialog(target); }
          catch (err) { setOwnershipError(err.message || "Couldn't verify that answer."); }
          finally { setOwnershipBusy(false); }
        }}>
          <button type="button" className="contact-close" onClick={() => setOwnershipChallenge(null)} aria-label="Close ownership check">×</button>
          <header className="ownership-dialog-head">
            <span className="ownership-shield" aria-hidden="true"><FactIcon size={25}><path d="M12 22s8-4 8-11V5l-8-3-8 3v6c0 7 8 11 8 11Z" /><path d="m9 12 2 2 4-4" /></FactIcon></span>
            <div className="ownership-heading"><p className="ownership-eyebrow">PRIVATE OWNERSHIP CHECK</p><h2 id="ownership-dialog-title">Confirm it’s yours</h2></div>
          </header>
          <div className="ownership-dialog-body">
            <p className="ownership-intro">The finder kept one identifying detail private. Answer correctly to continue to contact.</p>
            <div className="ownership-question"><FactIcon size={18}><circle cx="12" cy="12" r="9.5" /><path d="M9.7 9a2.4 2.4 0 1 1 4.2 1.6c-.9.9-1.9 1.2-1.9 2.9M12 17h.01" /></FactIcon><strong>{ownershipChallenge.question}</strong></div>
            <input className="ownership-answer-input" id="ownership-answer" autoComplete="off" value={ownershipAnswer} onChange={(e) => setOwnershipAnswer(e.target.value)} maxLength={200} placeholder="Type your answer…" required />
            {ownershipError && <p className="ownership-error" role="alert"><FactIcon size={17}><path d="M10.3 3.9 2.6 17.2A2 2 0 0 0 4.3 20h15.4a2 2 0 0 0 1.7-2.8L13.7 3.9a2 2 0 0 0-3.4 0Z" /><path d="M12 9v4m0 3h.01" /></FactIcon>{ownershipError}</p>}
            <div className="contact-actions"><button type="button" className="secondary" onClick={() => setOwnershipChallenge(null)}>Cancel</button><button className="primary" disabled={ownershipBusy}>{ownershipBusy ? "Checking…" : "Verify and continue"}</button></div>
          </div>
        </form>
      </div>}

      <SiteFooter navigate={navigate} />
      <Toast msg={toast} />

    </div>

  </MotionConfig >;

}

