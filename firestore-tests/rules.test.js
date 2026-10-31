import { readFileSync } from 'node:fs';
import { after, before, beforeEach, describe, test } from 'node:test';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import {
  collection,
  collectionGroup,
  deleteDoc,
  doc,
  getDoc,
  getDocs,
  query,
  serverTimestamp,
  setDoc,
  Timestamp,
  updateDoc,
  where,
  writeBatch,
} from 'firebase/firestore';

const OWNER = 'owner';
const MEMBER = 'member';
const STRANGER = 'stranger';
const GROUP = 'g1';
const ROUND = 'r1';
const CODE = 'K7QW2XPM';
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

const inMinutes = (minutes) => Timestamp.fromMillis(Date.now() + minutes * 60_000);
const secondsAgo = (seconds) => Timestamp.fromMillis(Date.now() - seconds * 1000);

async function seed(path, data) {
  await env.withSecurityRulesDisabled((ctx) => setDoc(doc(ctx.firestore(), path), data));
}

// Exactly the map FirebaseAuthRepository.toFirestoreMap() writes on first sign-in.
const loginUser = (displayName = 'Julian') => ({ displayName, createdAt: NOW });

const member = (uid, role = 'MEMBER') => ({
  userId: uid,
  role,
  displayName: uid,
  joinedAt: Timestamp.fromMillis(NOW),
});

/** Group with OWNER and MEMBER, seeded without rule checks. */
async function seedGroup() {
  await seed(`groups/${GROUP}`, { name: 'Familie', createdBy: OWNER, createdAt: Timestamp.fromMillis(NOW) });
  await seed(`groups/${GROUP}/members/${OWNER}`, member(OWNER, 'OWNER'));
  await seed(`groups/${GROUP}/members/${MEMBER}`, member(MEMBER));
}

async function seedInvite(extra = {}) {
  await seed(`inviteCodes/${CODE}`, {
    groupId: GROUP,
    groupName: 'Familie',
    createdBy: OWNER,
    createdAt: Timestamp.fromMillis(NOW),
    expiresAt: inMinutes(60),
    ...extra,
  });
}

/** Round as the startRound Cloud Function would write it, plus its content documents. */
async function seedRound(extra = {}) {
  const base = `groups/${GROUP}/rounds/${ROUND}`;
  await seed(base, {
    createdBy: OWNER,
    createdAt: Timestamp.fromMillis(NOW),
    difficulty: 'MIXED',
    questionCount: 2,
    answerCounts: [4, 3],
    secondsPerQuestion: 20,
    endsAt: inMinutes(60),
    ...extra,
  });
  await seed(`${base}/content/questions`, { items: [{ title: 'Q1' }, { title: 'Q2' }] });
  await seed(`${base}/content/solution`, { correct: [1, 2] });
}

const participantPath = (uid) => `groups/${GROUP}/rounds/${ROUND}/participants/${uid}`;

describe('users', () => {
  test('sign-in creates and reads the own document', async () => {
    const fs = db('alice');
    await assertSucceeds(setDoc(doc(fs, 'users/alice'), loginUser()));
    await assertSucceeds(getDoc(doc(fs, 'users/alice')));
  });

  test('empty displayName (Google account without a name) is allowed', async () => {
    await assertSucceeds(setDoc(doc(db('alice'), 'users/alice'), loginUser('')));
  });

  test('older documents with role/familyId stay readable', async () => {
    await seed('users/alice', { ...loginUser(), role: 'PARENT', familyId: null });
    await assertSucceeds(getDoc(doc(db('alice'), 'users/alice')));
  });

  test('no access to other users', async () => {
    await assertFails(setDoc(doc(db('bob'), 'users/alice'), loginUser()));
    await seed('users/alice', loginUser());
    await assertFails(getDoc(doc(db('bob'), 'users/alice')));
    await assertFails(updateDoc(doc(db('bob'), 'users/alice'), { displayName: 'x' }));
    await assertFails(getDoc(doc(anon(), 'users/alice')));
  });

  test('no additional personal fields', async () => {
    const fs = db('alice');
    await assertFails(setDoc(doc(fs, 'users/alice'), { ...loginUser(), email: 'a@b.de' }));
    await assertFails(setDoc(doc(fs, 'users/alice'), { ...loginUser(), role: 'ADMIN' }));
    await assertFails(setDoc(doc(fs, 'users/alice'), loginUser('x'.repeat(51))));
    await seed('users/alice', loginUser());
    await assertFails(updateDoc(doc(fs, 'users/alice'), { photoUrl: 'https://x' }));
    await assertFails(updateDoc(doc(fs, 'users/alice'), { createdAt: 1 }));
  });

  test('changing displayName is allowed', async () => {
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

  test('only the own progress is readable and writable', async () => {
    await assertSucceeds(setDoc(doc(db('alice'), 'users/alice/progress/nature'), progress()));
    await assertSucceeds(getDoc(doc(db('alice'), 'users/alice/progress/nature')));
    await assertFails(getDoc(doc(db('bob'), 'users/alice/progress/nature')));
    await assertFails(setDoc(doc(db('bob'), 'users/alice/progress/space'), progress()));
  });

  test('at most one more level per write, never one less', async () => {
    await seed('users/alice/progress/nature', progress({ completedLevels: 3 }));
    const ref = doc(db('alice'), 'users/alice/progress/nature');
    await assertSucceeds(updateDoc(ref, { completedLevels: 4, solvedInCurrentLevel: [] }));
    await assertFails(updateDoc(ref, { completedLevels: 6 }));
    await assertFails(updateDoc(ref, { completedLevels: 2 }));
    await assertFails(
      setDoc(doc(db('alice'), 'users/alice/progress/space'), progress({ completedLevels: 5 })),
    );
  });

  test('no extra fields and at most 20 solved questions', async () => {
    const ref = doc(db('alice'), 'users/alice/progress/nature');
    await assertFails(setDoc(ref, progress({ rank: 'MASTER' })));
    const tooMany = Array.from({ length: 21 }, (_, i) => `q${i}`);
    await assertFails(setDoc(ref, progress({ solvedInCurrentLevel: tooMany })));
  });
});

describe('groups', () => {
  const newGroup = (fs, groupId = GROUP, uid = OWNER) => {
    const batch = writeBatch(fs);
    batch.set(doc(fs, 'groups', groupId), { name: 'Freunde', createdBy: uid, createdAt: serverTimestamp() });
    batch.set(doc(fs, 'groups', groupId, 'members', uid), {
      userId: uid,
      role: 'OWNER',
      displayName: 'Julian',
      joinedAt: serverTimestamp(),
    });
    return batch;
  };

  test('create a group and become its owner in one batch', async () => {
    await assertSucceeds(newGroup(db(OWNER)).commit());
  });

  test('a group cannot be created without the owner membership', async () => {
    await assertFails(
      setDoc(doc(db(OWNER), 'groups', GROUP), { name: 'x', createdBy: OWNER, createdAt: serverTimestamp() }),
    );
  });

  test('nobody becomes owner of an existing or foreign group', async () => {
    await seedGroup();
    await assertFails(setDoc(doc(db(STRANGER), 'groups', GROUP, 'members', STRANGER), {
      ...member(STRANGER, 'OWNER'),
      joinedAt: serverTimestamp(),
    }));
    await assertFails(newGroup(db(STRANGER), 'g2', OWNER).commit());
  });

  test('only members read', async () => {
    await seedGroup();
    await assertSucceeds(getDoc(doc(db(MEMBER), 'groups', GROUP)));
    await assertSucceeds(getDocs(collection(db(MEMBER), 'groups', GROUP, 'members')));
    await assertFails(getDoc(doc(db(STRANGER), 'groups', GROUP)));
    await assertFails(getDoc(doc(db(STRANGER), 'groups', GROUP, 'members', OWNER)));
  });

  test('collection group query finds only the own memberships', async () => {
    await seedGroup();
    await seed(`groups/g2/members/${MEMBER}`, member(MEMBER));
    const fs = db(MEMBER);
    const mine = await assertSucceeds(
      getDocs(query(collectionGroup(fs, 'members'), where('userId', '==', MEMBER))),
    );
    if (mine.size !== 2) throw new Error(`expected 2 memberships, got ${mine.size}`);
    await assertFails(getDocs(collectionGroup(fs, 'members')));
  });

  test('only the owner renames, and only the name', async () => {
    await seedGroup();
    await assertSucceeds(updateDoc(doc(db(OWNER), 'groups', GROUP), { name: 'Neu' }));
    await assertFails(updateDoc(doc(db(OWNER), 'groups', GROUP), { createdBy: MEMBER }));
    await assertFails(updateDoc(doc(db(MEMBER), 'groups', GROUP), { name: 'Hack' }));
    await assertFails(deleteDoc(doc(db(MEMBER), 'groups', GROUP)));
  });

  test('the owner cannot add people directly, only via invite code', async () => {
    await seedGroup();
    await assertFails(setDoc(doc(db(OWNER), 'groups', GROUP, 'members', 'new'), {
      ...member('new'),
      joinedAt: serverTimestamp(),
    }));
  });

  test('members update only their own displayName', async () => {
    await seedGroup();
    const own = doc(db(MEMBER), 'groups', GROUP, 'members', MEMBER);
    await assertSucceeds(updateDoc(own, { displayName: 'Jules' }));
    await assertFails(updateDoc(own, { role: 'OWNER' }));
    await assertFails(updateDoc(doc(db(MEMBER), 'groups', GROUP, 'members', OWNER), { displayName: 'x' }));
  });

  test('owner removes members, members leave on their own', async () => {
    await seedGroup();
    await seed(`groups/${GROUP}/members/third`, member('third'));
    await assertFails(deleteDoc(doc(db(MEMBER), 'groups', GROUP, 'members', OWNER)));
    await assertFails(deleteDoc(doc(db(MEMBER), 'groups', GROUP, 'members', 'third')));
    await assertSucceeds(deleteDoc(doc(db(OWNER), 'groups', GROUP, 'members', 'third')));
    await assertSucceeds(deleteDoc(doc(db(MEMBER), 'groups', GROUP, 'members', MEMBER)));
  });

  test('the owner leaves only by deleting the whole group', async () => {
    await seedGroup();
    const fs = db(OWNER);
    await assertFails(deleteDoc(doc(fs, 'groups', GROUP, 'members', OWNER)));
    const batch = writeBatch(fs);
    batch.delete(doc(fs, 'groups', GROUP, 'members', MEMBER));
    batch.delete(doc(fs, 'groups', GROUP, 'members', OWNER));
    batch.delete(doc(fs, 'groups', GROUP));
    await assertSucceeds(batch.commit());
  });
});

describe('invite codes', () => {
  const invite = (extra = {}) => ({
    groupId: GROUP,
    groupName: 'Familie',
    createdBy: OWNER,
    createdAt: serverTimestamp(),
    expiresAt: inMinutes(60 * 24),
    ...extra,
  });

  const join = (uid, code = CODE) => setDoc(doc(db(uid), 'groups', GROUP, 'members', uid), {
    ...member(uid),
    joinedAt: serverTimestamp(),
    inviteCode: code,
  });

  test('only the owner creates codes, valid for at most 7 days', async () => {
    await seedGroup();
    await assertSucceeds(setDoc(doc(db(OWNER), 'inviteCodes', CODE), invite()));
    await assertFails(setDoc(doc(db(MEMBER), 'inviteCodes', 'M7QW2XPM'), invite({ createdBy: MEMBER })));
    await assertFails(setDoc(doc(db(OWNER), 'inviteCodes', 'P7QW2XPM'), invite({ expiresAt: inMinutes(60 * 24 * 8) })));
  });

  test('codes must have the expected format', async () => {
    await seedGroup();
    await assertFails(setDoc(doc(db(OWNER), 'inviteCodes', 'ABC'), invite()));
    await assertFails(setDoc(doc(db(OWNER), 'inviteCodes', 'K7QW2XP0'), invite()));
  });

  test('anyone signed in can look up a code, but not list codes', async () => {
    await seedGroup();
    await seedInvite();
    await assertSucceeds(getDoc(doc(db(STRANGER), 'inviteCodes', CODE)));
    await assertFails(getDoc(doc(anon(), 'inviteCodes', CODE)));
    await assertFails(getDocs(collection(db(STRANGER), 'inviteCodes')));
    await assertSucceeds(
      getDocs(query(collection(db(OWNER), 'inviteCodes'), where('createdBy', '==', OWNER))),
    );
  });

  test('joining with a valid code makes you a member', async () => {
    await seedGroup();
    await seedInvite();
    await assertSucceeds(join(STRANGER));
    await assertSucceeds(getDoc(doc(db(STRANGER), 'groups', GROUP)));
  });

  test('joining fails with an unknown, expired or foreign code, or as owner', async () => {
    await seedGroup();
    await assertFails(join(STRANGER));
    await seedInvite({ expiresAt: secondsAgo(1) });
    await assertFails(join(STRANGER));
    await seedInvite({ groupId: 'other' });
    await assertFails(join(STRANGER));
    await seedInvite();
    await assertFails(setDoc(doc(db(STRANGER), 'groups', GROUP, 'members', STRANGER), {
      ...member(STRANGER, 'OWNER'),
      joinedAt: serverTimestamp(),
      inviteCode: CODE,
    }));
  });

  test('codes are never edited, only deleted by the owner', async () => {
    await seedGroup();
    await seedInvite();
    await assertFails(updateDoc(doc(db(OWNER), 'inviteCodes', CODE), { expiresAt: inMinutes(60 * 24 * 30) }));
    await assertFails(deleteDoc(doc(db(MEMBER), 'inviteCodes', CODE)));
    await assertSucceeds(deleteDoc(doc(db(OWNER), 'inviteCodes', CODE)));
  });
});

describe('group mode rounds', () => {
  const start = (uid) => setDoc(doc(db(uid), participantPath(uid)), {
    displayName: uid,
    startedAt: serverTimestamp(),
    answers: {},
  });

  const answer = (uid, index, choice) => updateDoc(doc(db(uid), participantPath(uid)), {
    [`answers.${index}`]: { choice, at: serverTimestamp() },
  });

  test('clients never write rounds or read the question pool', async () => {
    await seedGroup();
    await assertFails(setDoc(doc(db(OWNER), 'groups', GROUP, 'rounds', 'r2'), { questionCount: 1 }));
    await seed('groupQuestions/gq1', { title: 'x', correctAnswerIndex: 0 });
    await assertFails(getDoc(doc(db(OWNER), 'groupQuestions/gq1')));
  });

  test('members see the round, questions only after starting', async () => {
    await seedGroup();
    await seedRound();
    const questions = `groups/${GROUP}/rounds/${ROUND}/content/questions`;
    await assertSucceeds(getDoc(doc(db(MEMBER), `groups/${GROUP}/rounds/${ROUND}`)));
    await assertFails(getDoc(doc(db(STRANGER), `groups/${GROUP}/rounds/${ROUND}`)));
    await assertFails(getDoc(doc(db(MEMBER), questions)));
    await assertSucceeds(start(MEMBER));
    await assertSucceeds(getDoc(doc(db(MEMBER), questions)));
    await assertFails(start(STRANGER));
  });

  test('starting is only possible before the round ends, and only once', async () => {
    await seedGroup();
    await seedRound({ endsAt: secondsAgo(1) });
    await assertFails(start(MEMBER));
    await seedRound();
    await assertSucceeds(start(MEMBER));
    await assertFails(start(MEMBER));
  });

  test('answers are appended in order within the timer', async () => {
    await seedGroup();
    await seedRound();
    await start(MEMBER);
    await assertFails(answer(MEMBER, 1, 0));
    await assertFails(answer(MEMBER, 0, 4));
    await assertSucceeds(answer(MEMBER, 0, 1));
    await assertFails(answer(MEMBER, 0, 2));
    await assertSucceeds(answer(MEMBER, 1, 2));
    await assertFails(answer(MEMBER, 2, 0));
  });

  test('a late answer is rejected, a timeout marker is accepted', async () => {
    await seedGroup();
    await seedRound();
    await seed(participantPath(MEMBER), { displayName: MEMBER, startedAt: secondsAgo(60), answers: {} });
    await assertFails(answer(MEMBER, 0, 1));
    await assertSucceeds(answer(MEMBER, 0, -1));
  });

  test('nobody writes for someone else', async () => {
    await seedGroup();
    await seedRound();
    await start(MEMBER);
    await assertFails(updateDoc(doc(db(OWNER), participantPath(MEMBER)), {
      'answers.0': { choice: 1, at: serverTimestamp() },
    }));
  });

  test('solution and other answers only after finishing or after the end', async () => {
    await seedGroup();
    await seedRound();
    const solution = `groups/${GROUP}/rounds/${ROUND}/content/solution`;
    await start(OWNER);
    await answer(OWNER, 0, 1);
    await answer(OWNER, 1, 0);
    await start(MEMBER);
    await assertSucceeds(getDoc(doc(db(MEMBER), participantPath(MEMBER))));
    await assertFails(getDoc(doc(db(MEMBER), solution)));
    await assertFails(getDoc(doc(db(MEMBER), participantPath(OWNER))));
    await answer(MEMBER, 0, 1);
    await answer(MEMBER, 1, 2);
    await assertSucceeds(getDoc(doc(db(MEMBER), solution)));
    await assertSucceeds(getDocs(collection(db(MEMBER), `groups/${GROUP}/rounds/${ROUND}/participants`)));
  });

  test('after the end every member sees the results', async () => {
    await seedGroup();
    await seedRound({ endsAt: secondsAgo(1) });
    await seed(participantPath(OWNER), { displayName: OWNER, startedAt: secondsAgo(120), answers: {} });
    await assertSucceeds(getDoc(doc(db(MEMBER), `groups/${GROUP}/rounds/${ROUND}/content/solution`)));
    await assertSucceeds(getDoc(doc(db(MEMBER), participantPath(OWNER))));
    await assertFails(getDoc(doc(db(STRANGER), participantPath(OWNER))));
  });
});

describe('content', () => {
  test('published questions are public, drafts are not', async () => {
    await seed('questions/pub', { title: 'Warum ist der Himmel blau?', published: true });
    await seed('questions/draft', { title: 'Entwurf', published: false });
    await assertSucceeds(getDoc(doc(anon(), 'questions/pub')));
    await assertSucceeds(
      getDocs(query(collection(anon(), 'questions'), where('published', '==', true))),
    );
    await assertFails(getDoc(doc(db('alice'), 'questions/draft')));
  });

  test('clients write neither questions nor categories', async () => {
    await assertFails(setDoc(doc(db('alice'), 'questions/q1'), { published: true }));
    await assertFails(setDoc(doc(db('alice'), 'categories/c1'), { name: 'Natur' }));
  });

  test('unknown and retired collections are denied', async () => {
    await assertFails(setDoc(doc(db('alice'), 'families/f1'), { name: 'x' }));
    await assertFails(setDoc(doc(db('alice'), 'challenges/c1'), { groupId: GROUP }));
  });
});
