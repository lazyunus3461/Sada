package tr.kasaba.hub;

import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class PartyBookTest {
    final AtomicLong time=new AtomicLong();final PartyBook book=new PartyBook(time::get);final UUID a=UUID.randomUUID(),b=UUID.randomUUID(),c=UUID.randomUUID();
    @Test void invitationIsRequiredSingleUseAndExpiresAtTheDeadline(){
        book.create(a);assertThrows(IllegalArgumentException.class,()->book.accept(b,a,8));book.invite(a,b,8,60);time.set(TimeUnit.SECONDS.toNanos(60));assertThrows(IllegalArgumentException.class,()->book.accept(b,a,8));
        book.invite(a,b,8,60);book.accept(b,a,8);assertEquals(book.party(a),book.party(b));assertThrows(IllegalArgumentException.class,()->book.accept(b,a,8));
    }
    @Test void concurrentInvitesCannotExceedCapacityOnAcceptance(){
        book.create(a);book.invite(a,b,2,60);book.invite(a,c,2,60);book.accept(b,a,2);assertThrows(IllegalArgumentException.class,()->book.accept(c,a,2));assertNull(book.party(c));assertEquals(2,book.party(a).members().size());
    }
    @Test void aPlayerCanBelongToOnlyOneGroup(){
        book.create(a);book.create(b);assertThrows(IllegalArgumentException.class,()->book.invite(a,b,8,60));assertThrows(IllegalArgumentException.class,()->book.create(a));
    }
    @Test void nonLeaderCannotInviteKickOrDisband(){
        book.create(a);book.invite(a,b,8,60);book.accept(b,a,8);
        assertThrows(IllegalArgumentException.class,()->book.invite(b,c,8,60));assertThrows(IllegalArgumentException.class,()->book.kick(b,a));assertThrows(IllegalArgumentException.class,()->book.disband(b));
    }
    @Test void transferInvalidatesThePreviousLeadersInvites(){
        book.create(a);book.invite(a,b,8,60);book.accept(b,a,8);book.invite(a,c,8,60);book.transfer(a,b);assertThrows(IllegalArgumentException.class,()->book.accept(c,b,8));assertThrows(IllegalArgumentException.class,()->book.accept(c,a,8));
    }
    @Test void disconnectPromotesTheNextMemberAndLastLeaveRemovesTheParty(){
        book.create(a);book.invite(a,b,8,60);book.accept(b,a,8);var next=book.leave(a);assertEquals(b,next.leader());assertNull(book.party(a));assertNull(book.leave(b));assertNull(book.party(b));
    }
    @Test void kickAndDisbandDoNotLeaveGhostMemberships(){
        book.create(a);book.invite(a,b,8,60);book.accept(b,a,8);book.kick(a,b);assertNull(book.party(b));book.create(b);book.disband(a);assertNull(book.party(a));assertNotNull(book.party(b));
    }
    @Test void disconnectAndRecreateCannotResurrectOldInvites(){
        book.create(a);book.invite(a,b,8,60);book.leave(a);book.create(a);assertThrows(IllegalArgumentException.class,()->book.accept(b,a,8));book.prune();
    }
}
