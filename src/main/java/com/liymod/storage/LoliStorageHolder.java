package com.liymod.storage;

/** One decoded storage per owning stack; stack copies receive their own independent cache. */
public interface LoliStorageHolder {
    LoliStorageData liymod$getStorage();
    void liymod$setStorage(LoliStorageData storage);
}
