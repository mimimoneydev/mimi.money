package com.money.mimi.helpers.Files.backup;

import com.money.mimi.models.messages.MessagesModel;

import io.realm.DynamicRealm;
import io.realm.RealmMigration;
import io.realm.RealmObjectSchema;
import io.realm.RealmSchema;

/**
 * Created by Abderrahim El imame on 12/5/17.
 *
 * @Email : abderrahim.elimame@gmail.com
 * @Author : https://twitter.com/Ben__Cherif
 * @Skype : ben-_-cherif
 */

public class RealmMigrations implements RealmMigration {

    @Override
    public void migrate(DynamicRealm realm, long oldVersion, long newVersion) {
        final RealmSchema schema = realm.getSchema();

        if (oldVersion == 1) {//old database version
            final RealmObjectSchema userSchema = schema.get("ConversationsModel");
            userSchema.addField("Message", MessagesModel.class);
            userSchema.addField("online", boolean.class);
        }
    }
}