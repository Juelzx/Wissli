import { readFileSync } from 'node:fs';
import { after, before, beforeEach, describe, test } from 'node:test';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import {
  Timestamp,
  collection,
  collectionGroup,
  deleteDoc,
  doc,
  getDoc,
  getDocs,
  query,
  setDoc,
  updateDoc,
  where,
  writeBatch,
} from 'firebase/firestore';

const OWNER = 'owner';
const MEMBER = 'member';
const STRANGER = 'stranger';
const GROUP = 'g1';
const INVITE = 'invite-code-0123456789';
const NOW = 1_700_000_000_000;

let env;

before(async () => {
  env = await initializeTestEnvironment({
    projectId: 'demo-wissli',
    firestore: { rules: readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8') },
  });
});

after(async () => {
  await env.cleanup();
});

beforeEach(async () => {
  await env.clearFirestore();
});

const db = (uid, claims) => env.authenticatedContext(uid, claims).firestore();
const anon = () => env.unauthenticatedContext().firestore();

// Genau die Map, die FirebaseAuthRepository.toFirestoreMap() beim Erst-Login schreibt.
const loginUser = (displayName = 'Julian') => ({
  displayName,
  role: 'PARENT',
  familyId: null,
  createdAt: NOW,
});

const member = (uid, extra = {}) => ({ userId: uid, displayName: uid, joinedAt: NOW, ...extra });

const pendingChallenge = (extra = {}) => ({
  questionId: 'q1',
  createdBy: OWNER,
  assignedTo: MEMBER,
  status: 'PENDING',
  answer: null,
  result: null,
  createdAt: NOW,
  completedAt: null,
  ...extra,
});

/** Gruppe mit OWNER und MEMBER ohne Regelprüfung anlegen. */
async function seedGroup() {
  await env.withSecurityRulesDisabled(async (ctx) => {
    const fs = ctx.firestore();
    await setDoc(doc(fs, 'groups', GROUP), { name: 'Familie', ownerId: OWNER, createdAt: NOW });
    await setDoc(doc(fs, 'groups', GROUP, 'members', OWNER), member(OWNER));
    await setDoc(doc(fs, 'groups', GROUP, 'members', MEMBER), member(MEMBER));
  });
}

async function seed(path, data) {
  await env.withSecurityRulesDisabled((ctx) => setDoc(doc(ctx.firestore(), path), data));
}

describe('users', () => {
  test('Login-Erstanlage des eigenen Dokuments ist erlaubt', async () => {
    await assertSucceeds(setDoc(doc(db('alice'), 'users/alice'), loginUser()));
  });

  test('leerer displayName (Google-Account ohne Namen) ist erlaubt', async () => {
    await assertSucceeds(setDoc(doc(db('alice'), 'users/alice'), loginUser('')));
  });

  test('fremdes Dokument anlegen, lesen, ändern ist verboten', async () => {
    await assertFails(setDoc(doc(db('bob'), 'users/alice'), loginUser()));
    await seed('users/alice', loginUser());
    await assertFails(getDoc(doc(db('bob'), 'users/alice')));
    await assertFails(updateDoc(doc(db('bob'), 'users/alice'), { displayName: 'x' }));
    await assertFails(deleteDoc(doc(db('bob'), 'users/alice')));
  });

  test('nicht angemeldet: kein Zugriff', async () => {
    await seed('users/alice', loginUser());
    await assertFails(getDoc(doc(anon(), 'users/alice')));
  });

  test('eigenes Dokument lesen, displayName ändern und löschen ist erlaubt', async () => {
    await seed('users/alice', loginUser());
    const fs = db('alice');
    await assertSucceeds(getDoc(doc(fs, 'users/alice')));
    await assertSucceeds(updateDoc(doc(fs, 'users/alice'), { displayName: 'Jules' }));
    await assertSucceeds(deleteDoc(doc(fs, 'users/alice')));
  });

  test('zusätzliche personenbezogene Felder sind verboten', async () => {
    const fs = db('alice');
    await assertFails(setDoc(doc(fs, 'users/alice'), { ...loginUser(), email: 'a@b.de' }));
    await seed('users/alice', loginUser());
    await assertFails(updateDoc(doc(fs, 'users/alice'), { photoUrl: 'https://x' }));
  });

  test('zu langer displayName ist verboten', async () => {
    await assertFails(setDoc(doc(db('alice'), 'users/alice'), loginUser('x'.repeat(51))));
  });

  test('Rolle ADMIN kann weder beim Anlegen noch später gesetzt werden', async () => {
    const fs = db('alice');
    await assertFails(setDoc(doc(fs, 'users/alice'), { ...loginUser(), role: 'ADMIN' }));
    await seed('users/alice', loginUser());
    await assertFails(updateDoc(doc(fs, 'users/alice'), { role: 'ADMIN' }));
    await assertFails(updateDoc(doc(fs, 'users/alice'), { role: 'CHILD' }));
  });

  test('familyId und createdAt sind nicht veränderbar', async () => {
    const fs = db('alice');
    await assertFails(setDoc(doc(fs, 'users/alice'), { ...loginUser(), familyId: 'f1' }));
    await seed('users/alice', loginUser());
    await assertFails(updateDoc(doc(fs, 'users/alice'), { familyId: 'f1' }));
    await assertFails(updateDoc(doc(fs, 'users/alice'), { createdAt: 1 }));
  });

  test('Favoriten gehören nur der eigenen Person', async () => {
    await assertSucceeds(setDoc(doc(db('alice'), 'users/alice/favorites/q1'), { createdAt: NOW }));
    await assertSucceeds(getDoc(doc(db('alice'), 'users/alice/favorites/q1')));
    await assertFails(getDoc(doc(db('bob'), 'users/alice/favorites/q1')));
    await assertFails(setDoc(doc(db('bob'), 'users/alice/favorites/q2'), { createdAt: NOW }));
    await assertFails(deleteDoc(doc(db('bob'), 'users/alice/favorites/q1')));
    await assertSucceeds(deleteDoc(doc(db('alice'), 'users/alice/favorites/q1')));
  });
});

describe('groups', () => {
  test('Gruppe anlegen und als Owner beitreten in einem Batch', async () => {
    const fs = db(OWNER);
    const batch = writeBatch(fs);
    batch.set(doc(fs, 'groups', GROUP), { name: 'Freunde', ownerId: OWNER, createdAt: NOW });
    batch.set(doc(fs, 'groups', GROUP, 'members', OWNER), member(OWNER));
    await assertSucceeds(batch.commit());
  });

  test('Gruppe nicht im Namen anderer anlegen', async () => {
    await assertFails(
      setDoc(doc(db(STRANGER), 'groups', GROUP), { name: 'x', ownerId: OWNER, createdAt: NOW }),
    );
  });

  test('nur Mitglieder lesen Gruppe und Mitgliederliste', async () => {
    await seedGroup();
    await assertSucceeds(getDoc(doc(db(MEMBER), 'groups', GROUP)));
    await assertSucceeds(getDocs(query(collection(db(MEMBER), 'groups', GROUP, 'members'))));
    await assertFails(getDoc(doc(db(STRANGER), 'groups', GROUP)));
    await assertFails(getDoc(doc(db(STRANGER), 'groups', GROUP, 'members', OWNER)));
  });

  test('"Meine Gruppen" per Collection-Group-Abfrage', async () => {
    await seedGroup();
    const q = query(collectionGroup(db(MEMBER), 'members'), where('userId', '==', MEMBER));
    const snap = await assertSucceeds(getDocs(q));
    if (snap.size !== 1) throw new Error(`erwartet 1 Mitgliedschaft, war ${snap.size}`);
    await assertFails(
      getDocs(query(collectionGroup(db(STRANGER), 'members'), where('userId', '==', MEMBER))),
    );
  });

  test('nur der Owner benennt um; ownerId ist unveränderlich', async () => {
    await seedGroup();
    await assertSucceeds(updateDoc(doc(db(OWNER), 'groups', GROUP), { name: 'Neu' }));
    await assertFails(updateDoc(doc(db(MEMBER), 'groups', GROUP), { name: 'Hack' }));
    await assertFails(updateDoc(doc(db(OWNER), 'groups', GROUP), { ownerId: MEMBER }));
  });

  test('nur der Owner löscht die Gruppe', async () => {
    await seedGroup();
    await assertFails(deleteDoc(doc(db(MEMBER), 'groups', GROUP)));
    await assertSucceeds(deleteDoc(doc(db(OWNER), 'groups', GROUP)));
  });

  test('Beitritt nur mit gültiger Einladung', async () => {
    await seedGroup();
    const ref = doc(db(STRANGER), 'groups', GROUP, 'members', STRANGER);
    await assertFails(setDoc(ref, member(STRANGER)));
    await assertFails(setDoc(ref, member(STRANGER, { inviteCode: INVITE })));

    const future = Timestamp.fromMillis(Date.now() + 86_400_000);
    await assertSucceeds(
      setDoc(doc(db(OWNER), 'groups', GROUP, 'invites', INVITE), {
        createdAt: NOW,
        expiresAt: future,
      }),
    );
    await assertSucceeds(setDoc(ref, member(STRANGER, { inviteCode: INVITE })));
  });

  test('abgelaufene Einladung reicht nicht', async () => {
    await seedGroup();
    await seed(`groups/${GROUP}/invites/${INVITE}`, {
      createdAt: NOW,
      expiresAt: Timestamp.fromMillis(Date.now() - 1000),
    });
    await assertFails(
      setDoc(
        doc(db(STRANGER), 'groups', GROUP, 'members', STRANGER),
        member(STRANGER, { inviteCode: INVITE }),
      ),
    );
  });

  test('nur der Owner erstellt und sieht Einladungen', async () => {
    await seedGroup();
    const data = { createdAt: NOW, expiresAt: Timestamp.fromMillis(Date.now() + 60_000) };
    await assertFails(setDoc(doc(db(MEMBER), 'groups', GROUP, 'invites', INVITE), data));
    await assertFails(setDoc(doc(db(OWNER), 'groups', GROUP, 'invites', 'kurz'), data));
    await seed(`groups/${GROUP}/invites/${INVITE}`, data);
    await assertFails(getDoc(doc(db(STRANGER), 'groups', GROUP, 'invites', INVITE)));
    await assertFails(getDoc(doc(db(MEMBER), 'groups', GROUP, 'invites', INVITE)));
  });

  test('niemand trägt andere als Mitglied ein', async () => {
    await seedGroup();
    await assertFails(
      setDoc(doc(db(OWNER), 'groups', GROUP, 'members', STRANGER), member(STRANGER)),
    );
    await assertFails(
      setDoc(doc(db(STRANGER), 'groups', GROUP, 'members', STRANGER), member(OWNER)),
    );
  });

  test('Owner entfernt Mitglieder, Mitglieder treten selbst aus', async () => {
    await seedGroup();
    await assertFails(deleteDoc(doc(db(MEMBER), 'groups', GROUP, 'members', OWNER)));
    await assertFails(deleteDoc(doc(db(OWNER), 'groups', GROUP, 'members', OWNER)));
    await assertSucceeds(deleteDoc(doc(db(MEMBER), 'groups', GROUP, 'members', MEMBER)));
    await seed(`groups/${GROUP}/members/${MEMBER}`, member(MEMBER));
    await assertSucceeds(deleteDoc(doc(db(OWNER), 'groups', GROUP, 'members', MEMBER)));
  });

  test('Owner räumt eigene Mitgliedschaft nach Löschen der Gruppe ab', async () => {
    await seedGroup();
    await assertSucceeds(deleteDoc(doc(db(OWNER), 'groups', GROUP)));
    await assertSucceeds(deleteDoc(doc(db(OWNER), 'groups', GROUP, 'members', OWNER)));
  });

  test('Mitglied ändert nur den eigenen displayName', async () => {
    await seedGroup();
    const own = doc(db(MEMBER), 'groups', GROUP, 'members', MEMBER);
    await assertSucceeds(updateDoc(own, { displayName: 'Neu' }));
    await assertFails(updateDoc(own, { joinedAt: 1 }));
    await assertFails(
      updateDoc(doc(db(MEMBER), 'groups', GROUP, 'members', OWNER), { displayName: 'x' }),
    );
  });
});

describe('challenges', () => {
  const challengeRef = (uid, id = 'c1') => doc(db(uid), 'groups', GROUP, 'challenges', id);

  test('Mitglied fordert anderes Mitglied heraus', async () => {
    await seedGroup();
    await assertSucceeds(setDoc(challengeRef(OWNER), pendingChallenge()));
    // Keine Hierarchie: auch Nicht-Owner können herausfordern.
    await assertSucceeds(
      setDoc(challengeRef(MEMBER, 'c2'), pendingChallenge({ createdBy: MEMBER, assignedTo: OWNER })),
    );
  });

  test('ungültige Challenges werden abgelehnt', async () => {
    await seedGroup();
    await assertFails(setDoc(challengeRef(STRANGER), pendingChallenge({ createdBy: STRANGER })));
    await assertFails(setDoc(challengeRef(OWNER), pendingChallenge({ assignedTo: STRANGER })));
    await assertFails(setDoc(challengeRef(OWNER), pendingChallenge({ assignedTo: OWNER })));
    await assertFails(setDoc(challengeRef(OWNER), pendingChallenge({ createdBy: MEMBER })));
    await assertFails(setDoc(challengeRef(OWNER), pendingChallenge({ status: 'COMPLETED' })));
    await assertFails(setDoc(challengeRef(OWNER), pendingChallenge({ answer: 'x' })));
    await assertFails(setDoc(challengeRef(OWNER), pendingChallenge({ extra: 1 })));
  });

  test('nur Beteiligte lesen', async () => {
    await seedGroup();
    await seed(`groups/${GROUP}/members/third`, member('third'));
    await seed(`groups/${GROUP}/challenges/c1`, pendingChallenge());
    await assertSucceeds(getDoc(challengeRef(OWNER)));
    await assertSucceeds(getDoc(challengeRef(MEMBER)));
    await assertFails(getDoc(challengeRef('third')));
    await assertFails(getDoc(challengeRef(STRANGER)));
  });

  test('"Meine Challenges" per Collection-Group-Abfrage', async () => {
    await seedGroup();
    await seed(`groups/${GROUP}/challenges/c1`, pendingChallenge());
    const q = (uid) =>
      query(collectionGroup(db(uid), 'challenges'), where('assignedTo', '==', MEMBER));
    await assertSucceeds(getDocs(q(MEMBER)));
    await assertFails(getDocs(q(STRANGER)));
  });

  test('Herausgeforderte:r startet und schließt ab', async () => {
    await seedGroup();
    await seed(`groups/${GROUP}/challenges/c1`, pendingChallenge());
    await assertSucceeds(updateDoc(challengeRef(MEMBER), { status: 'IN_PROGRESS' }));
    await assertSucceeds(
      updateDoc(challengeRef(MEMBER), {
        status: 'COMPLETED',
        answer: 'Wegen Rayleigh-Streuung',
        result: 'CORRECT',
        completedAt: NOW + 1,
      }),
    );
    // Abgeschlossen ist abgeschlossen.
    await assertFails(updateDoc(challengeRef(MEMBER), { answer: 'anders' }));
  });

  test('Erstellende:r und Fremde können nicht antworten', async () => {
    await seedGroup();
    await seed(`groups/${GROUP}/challenges/c1`, pendingChallenge());
    const done = { status: 'COMPLETED', answer: 'x', completedAt: NOW };
    await assertFails(updateDoc(challengeRef(OWNER), done));
    await assertFails(updateDoc(challengeRef(STRANGER), done));
  });

  test('Herausgeforderte:r ändert keine Stammdaten', async () => {
    await seedGroup();
    await seed(`groups/${GROUP}/challenges/c1`, pendingChallenge());
    await assertFails(updateDoc(challengeRef(MEMBER), { questionId: 'q2' }));
    await assertFails(updateDoc(challengeRef(MEMBER), { assignedTo: OWNER }));
    await assertFails(updateDoc(challengeRef(MEMBER), { status: 'IN_PROGRESS', answer: 'x' }));
  });

  test('ausgetretenes Mitglied kann nicht mehr antworten', async () => {
    await seedGroup();
    await seed(`groups/${GROUP}/challenges/c1`, pendingChallenge());
    await env.withSecurityRulesDisabled((ctx) =>
      deleteDoc(doc(ctx.firestore(), 'groups', GROUP, 'members', MEMBER)),
    );
    await assertFails(updateDoc(challengeRef(MEMBER), { status: 'IN_PROGRESS' }));
  });

  test('Erstellende:r oder Owner löscht', async () => {
    await seedGroup();
    await seed(`groups/${GROUP}/challenges/c1`, pendingChallenge({ createdBy: MEMBER, assignedTo: OWNER }));
    await seed(`groups/${GROUP}/challenges/c2`, pendingChallenge({ createdBy: MEMBER, assignedTo: OWNER }));
    await assertFails(deleteDoc(challengeRef(STRANGER)));
    await assertSucceeds(deleteDoc(challengeRef(MEMBER)));
    await assertSucceeds(deleteDoc(challengeRef(OWNER, 'c2')));
  });
});

describe('Content', () => {
  test('veröffentlichte Fragen sind öffentlich lesbar, unveröffentlichte nicht', async () => {
    await seed('questions/pub', { title: 'Warum ist der Himmel blau?', published: true });
    await seed('questions/draft', { title: 'Entwurf', published: false });
    await assertSucceeds(getDoc(doc(anon(), 'questions/pub')));
    await assertFails(getDoc(doc(db('alice'), 'questions/draft')));
  });

  test('nur Admins (Custom Claim) schreiben Fragen und Kategorien', async () => {
    const data = { title: 'Neu', published: false };
    await assertFails(setDoc(doc(db('alice'), 'questions/q1'), data));
    await assertFails(setDoc(doc(db('alice'), 'categories/c1'), { name: 'Natur' }));
    await assertSucceeds(setDoc(doc(db('admin', { admin: true }), 'questions/q1'), data));
    await assertSucceeds(getDoc(doc(db('admin', { admin: true }), 'questions/q1')));
    await assertSucceeds(setDoc(doc(db('admin', { admin: true }), 'categories/c1'), { name: 'Natur' }));
    await assertSucceeds(getDoc(doc(anon(), 'categories/c1')));
  });

  test('alte Top-Level-Collections sind gesperrt', async () => {
    await assertFails(setDoc(doc(db('alice'), 'families/f1'), { name: 'x' }));
    await assertFails(setDoc(doc(db('alice'), 'favorites/f1'), { userId: 'alice' }));
    await assertFails(setDoc(doc(db('alice'), 'challenges/c1'), { createdBy: 'alice' }));
  });
});
