import { useEffect, useState } from 'react'
import { Activity, ArrowDownUp, ChevronDown, CircleHelp, Plus, Radio, RefreshCw, Trophy, X } from 'lucide-react'

type Batter = { name: string; runs: number; balls: number; fours: number; sixes: number; strikeRate: number }
type Bowler = { name: string; runsConceded: number; wickets: number; legalBalls: number; economy: number }
type Delivery = { id: number; overNumber: number; ballNumber: number; batter: string; bowler: string; runs: number; extras: number; extraType: string; wicket: boolean; recordedAt: string }
type Match = {
    id: number; title: string; teamOne: string; teamTwo: string; venue: string; format: string
    status: string; battingTeam: string; runs: number; wickets: number; completedOvers: number
    ballsInOver: number; target: number | null; striker: string; nonStriker: string
    currentBowler: string; summary: string; runRate: number; batters: Batter[]; bowlers: Bowler[]
    recentDeliveries: Delivery[]; startedAt: string
}

const api = async <T,>(path: string, options?: RequestInit): Promise<T> => {
    const response = await fetch(path, {
        ...options,
        headers: { 'Content-Type': 'application/json', ...options?.headers },
    })
    if (!response.ok) {
        const error = await response.json().catch(() => null)
        throw new Error(error?.detail ?? error?.message ?? `Request failed (${response.status})`)
    }
    return response.json() as Promise<T>
}

const shortName = (name: string) => name.split(' ').map((part) => part[0]).join('').slice(0, 3).toUpperCase()
const oversText = (overs: number, balls: number) => `${overs}.${balls}`
const ballLabel = (ball: Delivery) => ball.wicket ? 'W' : ball.extraType === 'WIDE' ? `${ball.runs + ball.extras}wd` : ball.extraType === 'NO_BALL' ? `${ball.runs + ball.extras}nb` : `${ball.runs + ball.extras}`

function App() {
    const [matches, setMatches] = useState<Match[]>([])
    const [activeId, setActiveId] = useState<number | null>(null)
    const [match, setMatch] = useState<Match | null>(null)
    const [streamReady, setStreamReady] = useState(false)
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState('')
    const [extraType, setExtraType] = useState('WIDE')
    const [extraRuns, setExtraRuns] = useState(1)
    const [replacement, setReplacement] = useState('')
    const [bowlerName, setBowlerName] = useState('')
    const [showNewMatch, setShowNewMatch] = useState(false)

    const refreshMatches = async (preferredId?: number) => {
        const data = await api<Match[]>('/api/matches')
        setMatches(data)
        const id = preferredId ?? activeId ?? data[0]?.id ?? null
        setActiveId(id)
        const selected = data.find((item) => item.id === id)
        if (selected) setMatch(selected)
    }

    useEffect(() => {
        refreshMatches().catch((reason: Error) => setError(reason.message))
        const stream = new EventSource('/api/events')
        stream.onopen = () => setStreamReady(true)
        stream.onerror = () => setStreamReady(false)
        stream.addEventListener('score-update', (event) => {
            const updated = JSON.parse((event as MessageEvent).data) as Match
            setMatches((previous) => previous.map((item) => item.id === updated.id ? updated : item))
            setMatch((current) => current?.id === updated.id ? updated : current)
        })
        const poll = window.setInterval(() => refreshMatches().catch(() => undefined), 20000)
        return () => { stream.close(); window.clearInterval(poll) }
    }, [])

    useEffect(() => { setBowlerName(match?.currentBowler ?? '') }, [match?.id, match?.currentBowler])

    const recordDelivery = async (runs: number, wicket = false, extras = 0, type = '') => {
        if (!match || busy) return
        setBusy(true)
        setError('')
        try {
            const updated = await api<Match>(`/api/matches/${match.id}/deliveries`, {
                method: 'POST',
                body: JSON.stringify({
                    batter: match.striker,
                    bowler: bowlerName || match.currentBowler,
                    runs,
                    extras,
                    extraType: type || null,
                    wicket,
                    replacementBatter: wicket ? replacement || null : null,
                }),
            })
            setMatch(updated)
            setMatches((previous) => previous.map((item) => item.id === updated.id ? updated : item))
            if (wicket) setReplacement('')
        } catch (reason) {
            setError(reason instanceof Error ? reason.message : 'Unable to record delivery')
        } finally {
            setBusy(false)
        }
    }

    const createMatch = async (event: React.FormEvent<HTMLFormElement>) => {
        event.preventDefault()
        const form = new FormData(event.currentTarget)
        setBusy(true)
        try {
            const created = await api<Match>('/api/matches', {
                method: 'POST',
                body: JSON.stringify(Object.fromEntries(form.entries())),
            })
            setMatches((previous) => [...previous, created])
            setActiveId(created.id)
            setMatch(created)
            setShowNewMatch(false)
        } catch (reason) {
            setError(reason instanceof Error ? reason.message : 'Unable to create match')
        } finally {
            setBusy(false)
        }
    }

    if (!match) return (
        <main className="loading-screen">
            <div className="brand-mark">B</div>
            <p>{error || 'Connecting to the scorebook…'}</p>
            {error && <button className="text-button" onClick={() => refreshMatches().catch((reason: Error) => setError(reason.message))}><RefreshCw size={15} /> Retry</button>}
        </main>
    )

    const battingTeamInitials = shortName(match.battingTeam)
    const deliveryGroups = new Map<number, Delivery[]>()
    match.recentDeliveries.forEach((ball) => deliveryGroups.set(ball.overNumber, [...(deliveryGroups.get(ball.overNumber) ?? []), ball]))

    return (
        <div className="app-shell">
            <header className="topbar">
                <a className="brand" href="#top" aria-label="Boundary home"><span className="brand-mark">B</span><span>BOUNDARY<span className="brand-period">.</span></span></a>
                <nav className="top-nav" aria-label="Main navigation"><a className="active" href="#score">Live score</a><a href="#scorecard">Scorecard</a></nav>
                <div className="top-actions"><span className={`connection ${streamReady ? 'connected' : ''}`}><span />{streamReady ? 'LIVE SYNC' : 'RECONNECTING'}</span><button className="icon-button" title="Refresh matches" onClick={() => refreshMatches().catch((reason: Error) => setError(reason.message))}><RefreshCw size={17} /></button><button className="avatar" title="Scorer profile">SC</button></div>
            </header>

            <main id="top" className="dashboard">
                <aside className="match-rail">
                    <div className="rail-heading"><div><p className="eyebrow">MATCH CENTRE</p><h1>On the field</h1></div><span className="count">{matches.length.toString().padStart(2, '0')}</span></div>
                    <div className="match-list">
                        {matches.map((item) => <button className={`match-item ${item.id === match.id ? 'selected' : ''}`} key={item.id} onClick={() => { setActiveId(item.id); setMatch(item) }}>
                            <div className="match-item-top"><span className={`match-state ${item.status === 'LIVE' ? 'is-live' : ''}`}><i />{item.status}</span><span>{item.format.split('·')[0].trim()}</span></div>
                            <div className="rail-team"><b className="team-chip">{shortName(item.battingTeam)}</b><span>{item.battingTeam}</span><strong>{item.runs}/{item.wickets}</strong></div>
                            <div className="rail-team muted"><b className="team-chip pale">{shortName(item.battingTeam === item.teamOne ? item.teamTwo : item.teamOne)}</b><span>{item.battingTeam === item.teamOne ? item.teamTwo : item.teamOne}</span><small>{oversText(item.completedOvers, item.ballsInOver)} ov</small></div>
                            <div className="match-item-bottom"><span>{item.venue.split(',').at(-1)?.trim()}</span><span>Details <ChevronDown size={12} /></span></div>
                        </button>)}
                    </div>
                    <button className="new-match" onClick={() => setShowNewMatch(true)}><Plus size={16} /> New match</button>
                    <div className="rail-footer"><CircleHelp size={15} /><span>Scoring desk</span><span className="version">v1.0</span></div>
                </aside>

                <section id="score" className="main-content">
                    <div className="page-heading"><div><div className="breadcrumb">MATCH CENTRE <span>/</span> {match.format.toUpperCase()}</div><h2>{match.teamOne} <span>vs</span> {match.teamTwo}</h2><p className="venue-line">{match.venue} <span>·</span> {match.format}</p></div><button className="match-menu" onClick={() => setShowNewMatch(true)}><Plus size={16} /> Add match</button></div>

                    <section className="score-hero" aria-label="Live match score">
                        <div className="hero-photo" role="img" aria-label="Cricket ground in play" />
                        <div className="score-topline"><span className="live-pill"><Radio size={13} /> LIVE</span><span>1ST INNINGS</span><span className="hero-time">{new Date(match.startedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} START</span></div>
                        <div className="score-main"><div className="batting-mark">{battingTeamInitials}</div><div className="score-readout"><p>{match.battingTeam} <span>batting</span></p><div className="score-number">{match.runs}<span>/{match.wickets}</span></div><div className="score-overs">{oversText(match.completedOvers, match.ballsInOver)} <span>OVERS</span><b>·</b> CRR {match.runRate.toFixed(2)}</div></div><div className="score-divider" /><div className="score-context"><span className="context-label">{match.target ? 'TARGET' : 'INNINGS STATUS'}</span><strong>{match.target ?? 'In progress'}</strong><span>{match.target ? `${Math.max(0, match.target - match.runs)} runs to win` : match.summary}</span></div></div>
                        <div className="score-bottom"><span><Trophy size={14} /> {match.summary}</span><span>LAST UPDATE <b>{new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</b></span></div>
                    </section>

                    <div className="stat-ribbon"><div><span>RUN RATE</span><strong>{match.runRate.toFixed(2)}</strong><small>runs per over</small></div><div><span>WICKETS</span><strong>{match.wickets}<i> / 10</i></strong><small>down this innings</small></div><div><span>PARTNERSHIP</span><strong>{match.batters.length > 1 ? `${match.batters.at(-1)?.runs ?? 0}` : '—'}<i> runs</i></strong><small>{match.striker} &amp; {match.nonStriker}</small></div><div><span>INNINGS</span><strong>{oversText(match.completedOvers, match.ballsInOver)}<i> ov</i></strong><small>{match.format}</small></div></div>

                    <section className="live-panel">
                        <div className="section-head"><div><p className="eyebrow">RIGHT NOW</p><h3>At the crease</h3></div><span className="updating"><Activity size={14} /> {streamReady ? 'Updating live' : 'Syncing'}</span></div>
                        <div className="crease-grid">
                            {[match.striker, match.nonStriker].map((name, index) => {
                                const stats = match.batters.find((player) => player.name === name)
                                return <div className={`player-line ${index === 0 ? 'on-strike' : ''}`} key={`${name}-${index}`}><div className="player-avatar">{shortName(name)}</div><div className="player-name"><strong>{name}{index === 0 && <span className="strike-star">*</span>}</strong><small>{index === 0 ? 'ON STRIKE' : 'NON-STRIKER'}</small></div><div className="player-score">{stats?.runs ?? 0}<small>({stats?.balls ?? 0})</small></div><div className="player-detail"><span>{stats?.fours ?? 0}<small>4s</small></span><span>{stats?.sixes ?? 0}<small>6s</small></span></div><div className="player-rate">{stats?.strikeRate.toFixed(1) ?? '0.0'}<small>SR</small></div></div>
                            })}
                            <div className="bowler-line"><div className="bowler-icon"><ArrowDownUp size={16} /></div><div className="player-name"><strong>{match.currentBowler}</strong><small>RIGHT-ARM FAST</small></div><div className="bowler-figures">{match.bowlers.find((player) => player.name === match.currentBowler)?.wickets ?? 0}<small>W</small><span>/</span>{match.bowlers.find((player) => player.name === match.currentBowler)?.runsConceded ?? 0}<small>R</small></div><div className="player-rate">{(() => { const bowler = match.bowlers.find((player) => player.name === match.currentBowler); return bowler ? `${Math.floor(bowler.legalBalls / 6)}.${bowler.legalBalls % 6}` : '0.0' })()}<small>OV</small></div></div>
                        </div>
                    </section>

                    <section className="recent-panel">
                        <div className="section-head compact"><div><p className="eyebrow">BALL BY BALL</p><h3>Recent deliveries</h3></div><span className="sequence-label">LATEST FIRST</span></div>
                        <div className="over-sequence">
                            {[...deliveryGroups.entries()].reverse().map(([over, balls]) => <div className="over-group" key={over}><span className="over-label">{over + 1}<small>OVER</small></span><div className="ball-list">{[...balls].reverse().map((ball) => <span key={ball.id} title={`${ball.batter}: ${ball.runs} runs${ball.extras ? ` + ${ball.extras} ${ball.extraType}` : ''}`} className={`ball ${ball.wicket ? 'wicket' : ball.runs === 4 || ball.runs === 6 ? 'boundary' : ''}`}>{ballLabel(ball)}</span>)}</div></div>)}
                            {deliveryGroups.size === 0 && <span className="empty-note">No deliveries recorded yet.</span>}
                        </div>
                    </section>

                    <section className="score-entry">
                        <div className="section-head entry-head"><div><p className="eyebrow">SCORING DESK</p><h3>Record delivery</h3></div><span className="entry-batter">Facing <b>{match.striker}</b></span></div>
                        <div className="entry-controls"><div className="run-buttons" aria-label="Record runs">{[0, 1, 2, 3, 4, 6].map((runs) => <button key={runs} disabled={busy} className={`run-button ${runs === 4 || runs === 6 ? 'boundary-button' : ''}`} onClick={() => recordDelivery(runs)}>{runs === 0 ? '•' : runs}</button>)}</div><div className="entry-fields"><label>Bowler<input value={bowlerName} onChange={(event) => setBowlerName(event.target.value)} aria-label="Current bowler" /></label><label>Next batter<input value={replacement} onChange={(event) => setReplacement(event.target.value)} placeholder="On wicket" aria-label="Replacement batter" /></label></div></div>
                        <div className="entry-bottom"><div className="extra-control"><label htmlFor="extra-type">EXTRAS</label><select id="extra-type" value={extraType} onChange={(event) => setExtraType(event.target.value)}><option value="WIDE">Wide</option><option value="NO_BALL">No ball</option><option value="BYE">Bye</option><option value="LEG_BYE">Leg bye</option></select><input type="number" min="1" max="7" aria-label="Extra runs" value={extraRuns} onChange={(event) => setExtraRuns(Number(event.target.value))} /><button disabled={busy} onClick={() => recordDelivery(0, false, Math.max(1, extraRuns), extraType)}>Add extra</button></div><button className="wicket-button" disabled={busy} onClick={() => recordDelivery(0, true)}><X size={15} /> Wicket</button></div>
                        {error && <p className="error-message" role="alert">{error}</p>}
                    </section>

                    <section id="scorecard" className="scorecard-panel">
                        <div className="section-head compact"><div><p className="eyebrow">INNINGS DETAILS</p><h3>Scorecard</h3></div><span className="batting-team-tag">{battingTeamInitials} BATTING</span></div>
                        <div className="table-scroll"><table><thead><tr><th>BATTER</th><th>R</th><th>B</th><th>4s</th><th>6s</th><th>SR</th></tr></thead><tbody>{match.batters.map((player) => <tr key={player.name}><td><strong>{player.name}</strong>{player.name === match.striker && <span className="table-star">*</span>}</td><td>{player.runs}</td><td>{player.balls}</td><td>{player.fours}</td><td>{player.sixes}</td><td>{player.strikeRate.toFixed(1)}</td></tr>)}{match.batters.length === 0 && <tr><td className="empty-note" colSpan={6}>Scorecard will fill as deliveries are recorded.</td></tr>}</tbody></table></div>
                        <div className="table-scroll bowling-table"><table><thead><tr><th>BOWLER</th><th>O</th><th>R</th><th>W</th><th>ECON</th></tr></thead><tbody>{match.bowlers.map((player) => <tr key={player.name}><td><strong>{player.name}</strong></td><td>{Math.floor(player.legalBalls / 6)}.{player.legalBalls % 6}</td><td>{player.runsConceded}</td><td>{player.wickets}</td><td>{player.economy.toFixed(2)}</td></tr>)}{match.bowlers.length === 0 && <tr><td className="empty-note" colSpan={5}>No bowling figures yet.</td></tr>}</tbody></table></div>
                    </section>
                    <footer className="page-footer"><span>BOUNDARY SCOREBOOK <b>·</b> {new Date().getFullYear()}</span><span>Match data saves automatically</span></footer>
                </section>
            </main>

            {showNewMatch && <div className="modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) setShowNewMatch(false) }}><form className="new-match-modal" onSubmit={createMatch}><div className="modal-head"><div><p className="eyebrow">SET UP A FIXTURE</p><h3>New match</h3></div><button type="button" className="icon-button" title="Close" onClick={() => setShowNewMatch(false)}><X size={18} /></button></div><label>Match title<input name="title" required placeholder="Team A vs Team B" /></label><div className="form-row"><label>Team one<input name="teamOne" required placeholder="Home side" /></label><label>Team two<input name="teamTwo" required placeholder="Away side" /></label></div><label>Venue<input name="venue" required placeholder="Ground, city" /></label><div className="form-row"><label>Format<input name="format" required defaultValue="T20" /></label><label>Batting first<input name="battingTeam" required placeholder="Enter team name" /></label></div><div className="form-row"><label>Opening batter<input name="striker" required placeholder="On strike" /></label><label>Non-striker<input name="nonStriker" required placeholder="At the other end" /></label></div><label>Opening bowler<input name="bowler" required placeholder="Bowler name" /></label><button className="create-button" type="submit" disabled={busy}>Create fixture</button></form></div>}
        </div>
    )
}

export default App