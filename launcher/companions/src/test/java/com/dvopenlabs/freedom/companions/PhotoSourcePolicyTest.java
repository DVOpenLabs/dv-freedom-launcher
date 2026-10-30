package com.dvopenlabs.freedom.companions;
import org.junit.Test;
import static org.junit.Assert.*;
public class PhotoSourcePolicyTest {
    @Test public void aDirectPhotoUnderTheSelectedFolderIsAllowed(){assertEquals("https://photos.example/family/one.jpg",PhotoSourcePolicy.child("https://photos.example/family/","/family/one.jpg"));}
    @Test public void credentialsNeverFollowRemoteHrefs(){for(String href:new String[]{"https://other.example/family/a.jpg","http://photos.example/family/a.jpg","../private.jpg","/family/%2e%2e.jpg","/family/sub%2fa.jpg","/family/a%5cb.jpg","/family/sub/a.jpg","/family/a.jpg?token=secret","/family/a.svg","https://user:pass@photos.example/family/a.jpg"})try{PhotoSourcePolicy.child("https://photos.example/family/",href);fail(href);}catch(IllegalArgumentException expected){}}
    @Test public void albumIdsAreNotPaths(){assertTrue(PhotoSourcePolicy.album("00000000-0000-0000-0000-000000000001"));assertFalse(PhotoSourcePolicy.album("../private"));}
}
