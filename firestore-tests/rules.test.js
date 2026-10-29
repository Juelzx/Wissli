import { readFileSync } from 'node:fs';
import { after, before, beforeEach, describe, test } from 'node:test';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import {
  collection,
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

const db = (uid) => env.authenticatedContext(uid).firestore();
const anon = () => env.unauthenticatedContext().firestore();

async function seed(path, data) {
  await env.withSecurityRulesDisabled((ctx) => setDoc(doc(ctx.firestore(), path), data));
}

// Genau die Map, die FirebaseAuthRepository.toFirestoreMap() beim Erst-Login schreibt.
const loginUser = (displayName = 'Julian') => ({ displayName, createdAt: NOW });

/** Gruppe mit OWNER und MEMBER ohne Regelprüfung anlegen. */
async function seedGroup() {
  await seed(`groups/${GROUP}`, { name: 'Familie', createdBy: OWNER, createdAt: NOW });
  await seed(`groups/${GROUP}/members/${OWNER}`, { role: 'OWNER', joinedAt: NOW });
  await seed(`groups/${GROUP}/members/${MEMBER}`, { role: 'MEMBER', joinedAt: NOW });
}

const challenge = (extra = {}) => ({
  questionId: 'q1',
  groupId: GROUP,
  createdBy: OWNER,
  assignedTo: MEMBER,
  status: 'PENDING',
  createdAt: NOW,
  ...extra,
});

describe('users', () => {
  test('Login legt das eigene Dokument an und liest es', async () => {
    const fs = db('alice');
    await assertSucceeds(setDoc(doc(fs, 'users/alice'), loginUser()));
    await assertSucceeds(getDoc(doc(fs, 'users/alice')));
  });

  test('leerer displayName (Google-Account ohne Namen) ist erlaubt', async () => {
    await assertSucceeds(setDoc(doc(db('alice'), 'users/alice'), loginUser('')));
  });

  test('ältere Dokumente mit role/familyId bleiben lesbar', async () => {
    await seed('users/alice', { ...loginUser(), role: 'PARENT', familyId: null });
    await assertSucceeds(getDoc(doc(db('alice'), 'users/alice')));
  });

  test('fremde Dokumente: kein Zugriff', async () => {
    await assertFails(setDoc(doc(db('bob'), 'users/alice'), loginUser()));
    await seed('users/alice', loginUser());
    await assertFails(getDoc(doc(db('bob'), 'users/alice')));
    await assertFails(updateDoc(doc(db('bob'), 'users/alice'), { displayName: 'x' }));
    await assertFails(getDoc(doc(anon(), 'users/alice')));
  });

  test('keine zusätzlichen personenbezogenen Felder', async () => {
    const fs = db('alice');
    await assertFails(setDoc(doc(fs, 'users/alice'), { ...loginUser(), email: 'a@b.de' }));
    await assertFails(setDoc(doc(fs, 'users/alice'), { ...loginUser(), role: 'ADMIN' }));
    await assertFails(setDoc(doc(fs, 'users/alice'), loginUser('x'.repeat(51))));
    await seed('users/alice', loginUser());
    await assertFails(updateDoc(doc(fs, 'users/alice'), { photoUrl: 'https://x' }));
    await assertFails(updateDoc(doc(fs, 'users/alice'), { createdAt: 1 }));
  });

  test('displayName ändern ist erlaubt', async () => {
    await seed('users/alice', loginUser());
    await assertSucceeds(updateDoc(doc(db('alice'), 'users/alice'), { displayName: 'Jules' }));
  });
});

describe('progress', () => {
  const progress = (extra = {}) => ({
    completedLevels: 0,
    solvedInCurrentLevel: ['q1'],
    updatedAt: NOW,
    ...extra,
  });

  test('nur der eigene Fortschritt ist les- und schreibbar', async () => {
    await assertSucceeds(setDoc(doc(db('alice'), 'users/alice/progress/nature'), progress()));
    await assertSucceeds(getDoc(doc(db('alice'), 'users/alice/progress/nature')));
    await assertFails(getDoc(doc(db('bob'), 'users/alice/progress/nature')));
    await assertFails(setDoc(doc(db('bob'), 'users/alice/progress/space'), progress()));
  });

  test('pro Schreibvorgang höchstens ein Level mehr, nie eins weniger', async () => {
    await seed('users/alice/progress/nature', progress({ completedLevels: 3 }));
    const ref = doc(db('alice'), 'users/alice/progress/nature');
    await assertSucceeds(updateDoc(ref, { completedLevels: 4, solvedInCurrentLevel: [] }));
    await assertFails(updateDoc(ref, { completedLevels: 6 }));
    await assertFails(updateDoc(ref, { completedLevels: 2 }));
    await assertFails(
      setDoc(doc(db('alice'), 'users/alice/progress/space'), progress({ completedLevels: 5 })),
    );
  });

  test('keine Zusatzfelder und höchstens 20 gelöste Fragen', async () => {
    const ref = doc(db('alice'), 'users/alice/progress/nature');
    await assertFails(setDoc(ref, progress({ rank: 'MASTER' })));
    const tooMany = Array.from({ length: 21 }, (_, i) => `q${i}`);
    await assertFails(setDoc(ref, progress({ solvedInCurrentLevel: tooMany })));
  });
});

describe('groups', () => {
  test('Gruppe anlegen und als Owner eintragen in einem Batch', async () => {
    const fs = db(OWNER);
    const batch = writeBatch(fs);
    batch.set(doc(fs, 'groups', GROUP), { name: 'Freunde', createdBy: OWNER, createdAt: NOW });
    batch.set(doc(fs, 'groups', GROUP, 'members', OWNER), { role: 'OWNER', joinedAt: NOW });
    await assertSucceeds(batch.commit());
  });

  test('nicht Owner einer fremden Gruppe werden', async () => {
    await seedGroup();
    await assertFails(
      setDoc(doc(db(STRANGER), 'groups', GROUP, 'members', STRANGER), { role: 'OWNER' }),
    );
    await assertFails(
      setDoc(doc(db(STRANGER), 'groups', 'g2'), { name: 'x', createdBy: OWNER, createdAt: NOW }),
    );
  });

  test('nur Mitglieder lesen', async () => {
    await seedGroup();
    await assertSucceeds(getDoc(doc(db(MEMBER), 'groups', GROUP)));
    await assertSucceeds(getDocs(collection(db(MEMBER), 'groups', GROUP, 'members')));
    await assertFails(getDoc(doc(db(STRANGER), 'groups', GROUP)));
    await assertFails(getDoc(doc(db(STRANGER), 'groups', GROUP, 'members', OWNER)));
  });

  test('nur der Owner verwaltet und lädt ein', async () => {
    await seedGroup();
    await assertSucceeds(updateDoc(doc(db(OWNER), 'groups', GROUP), { name: 'Neu' }));
    await assertFails(updateDoc(doc(db(MEMBER), 'groups', GROUP), { name: 'Hack' }));
    await assertSucceeds(
      setDoc(doc(db(OWNER), 'groups', GROUP, 'members', 'new'), { role: 'MEMBER' }),
    );
    await assertFails(
      setDoc(doc(db(MEMBER), 'groups', GROUP, 'members', 'other'), { role: 'MEMBER' }),
    );
    await assertFails(deleteDoc(doc(db(MEMBER), 'groups', GROUP)));
  });

  test('Owner entfernt Mitglieder, Mitglieder treten selbst aus', async () => {
    await seedGroup();
    await assertFails(deleteDoc(doc(db(MEMBER), 'groups', GROUP, 'members', OWNER)));
    await assertSucceeds(deleteDoc(doc(db(MEMBER), 'groups', GROUP, 'members', MEMBER)));
  });
});

describe('challenges', () => {
  test('Mitglied fordert anderes Mitglied heraus, auch ohne Owner zu sein', async () => {
    await seedGroup();
    await assertSucceeds(setDoc(doc(db(OWNER), 'challenges/c1'), challenge()));
    await assertSucceeds(
      setDoc(doc(db(MEMBER), 'challenges/c2'), challenge({ createdBy: MEMBER, assignedTo: OWNER })),
    );
  });

  test('keine Challenges von oder an Fremde', async () => {
    await seedGroup();
    await assertFails(setDoc(doc(db(STRANGER), 'challenges/c1'), challenge({ createdBy: STRANGER })));
    await assertFails(setDoc(doc(db(OWNER), 'challenges/c1'), challenge({ assignedTo: STRANGER })));
    await assertFails(setDoc(doc(db(OWNER), 'challenges/c1'), challenge({ createdBy: MEMBER })));
  });

  test('nur Gruppenmitglieder lesen und Beteiligte bearbeiten', async () => {
    await seedGroup();
    await seed('challenges/c1', challenge());
    await assertSucceeds(getDoc(doc(db(MEMBER), 'challenges/c1')));
    await assertFails(getDoc(doc(db(STRANGER), 'challenges/c1')));
    await assertSucceeds(updateDoc(doc(db(MEMBER), 'challenges/c1'), { status: 'IN_PROGRESS' }));
    await assertFails(updateDoc(doc(db(STRANGER), 'challenges/c1'), { status: 'COMPLETED' }));
  });
});

describe('Content', () => {
  test('veröffentlichte Fragen sind öffentlich lesbar, Entwürfe nicht', async () => {
    await seed('questions/pub', { title: 'Warum ist der Himmel blau?', published: true });
    await seed('questions/draft', { title: 'Entwurf', published: false });
    await assertSucceeds(getDoc(doc(anon(), 'questions/pub')));
    await assertSucceeds(
      getDocs(query(collection(anon(), 'questions'), where('published', '==', true))),
    );
    await assertFails(getDoc(doc(db('alice'), 'questions/draft')));
  });

  test('Clients schreiben weder Fragen noch Kategorien', async () => {
    await assertFails(setDoc(doc(db('alice'), 'questions/q1'), { published: true }));
    await assertFails(setDoc(doc(db('alice'), 'categories/c1'), { name: 'Natur' }));
  });

  test('unbekannte Collections sind gesperrt', async () => {
    await assertFails(setDoc(doc(db('alice'), 'families/f1'), { name: 'x' }));
  });
});
