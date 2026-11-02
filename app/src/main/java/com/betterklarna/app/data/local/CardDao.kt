package com.betterklarna.app.data.local

import androidx.room.*
import com.betterklarna.app.data.model.LoyaltyCard
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {
    @Query("SELECT * FROM loyalty_cards ORDER BY isFavorite DESC, LOWER(name) ASC")
    fun getAllCards(): Flow<List<LoyaltyCard>>

    @Query("SELECT * FROM loyalty_cards ORDER BY id ASC")
    suspend fun getAllCardsOnce(): List<LoyaltyCard>

    @Query("SELECT * FROM loyalty_cards WHERE isFavorite = 1 ORDER BY LOWER(name) ASC")
    fun getFavoriteCards(): Flow<List<LoyaltyCard>>

    @Query("SELECT * FROM loyalty_cards WHERE id = :id LIMIT 1")
    suspend fun getCardById(id: Long): LoyaltyCard?

    @Query("SELECT * FROM loyalty_cards WHERE name LIKE '%' || :query || '%' OR cardNumber LIKE '%' || :query || '%' ORDER BY isFavorite DESC, LOWER(name) ASC")
    fun searchCards(query: String): Flow<List<LoyaltyCard>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: LoyaltyCard): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<LoyaltyCard>): List<Long>

    @Update
    suspend fun updateCard(card: LoyaltyCard)

    @Delete
    suspend fun deleteCard(card: LoyaltyCard)

    @Query("DELETE FROM loyalty_cards WHERE id IN (:ids)")
    suspend fun deleteCardsByIds(ids: List<Long>)

    @Query("UPDATE loyalty_cards SET lastUsedAt = :timestamp WHERE id = :id")
    suspend fun updateLastUsed(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE loyalty_cards SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Long, isFavorite: Boolean)
}
