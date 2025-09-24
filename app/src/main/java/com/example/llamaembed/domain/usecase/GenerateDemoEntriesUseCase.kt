package com.example.llamaembed.domain.usecase

import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Use case for generating demo voice memo entries for testing semantic search
 *
 * Creates 20 realistic voice memo entries across different semantic categories:
 * - Work/Meetings (4 entries)
 * - Food/Cooking (4 entries)
 * - Health/Fitness (3 entries)
 * - Travel/Transportation (3 entries)
 * - Personal/Family (3 entries)
 * - Learning/Hobbies (3 entries)
 */
@Singleton
class GenerateDemoEntriesUseCase @Inject constructor(
    private val saveVoiceMemoUseCase: SaveVoiceMemoUseCase
) {
    companion object {
        private const val TAG = "GenerateDemoEntries"

        // Demo entries organized by semantic categories
        private val DEMO_ENTRIES = listOf(
            // Work/Meetings (4 entries)
            "Need to schedule the quarterly team meeting for next week and prepare the agenda",
            "Client presentation is due Friday, remember to include the latest sales figures and projections",
            "Budget review meeting went well, approved the marketing spend for Q2 campaigns",
            "Performance evaluation notes: team exceeded targets by 15 percent this quarter",

            // Food/Cooking (4 entries)
            "Buy ingredients for homemade pizza tonight - need mozzarella, tomatoes, and fresh basil",
            "Try that new pasta recipe from the cooking show, looks delicious and easy to make",
            "Great restaurant recommendation downtown, the seafood place on Main Street has amazing reviews",
            "Meal prep for the week: grilled chicken, quinoa, and roasted vegetables for healthy lunches",

            // Health/Fitness (3 entries)
            "Start morning jog routine tomorrow at 6 AM, aim for 30 minutes around the park",
            "Doctor appointment reminder: annual checkup scheduled for next Thursday at 2 PM",
            "Downloaded new meditation app, going to try the 10-minute morning mindfulness session",

            // Travel/Transportation (3 entries)
            "Book train tickets for the weekend getaway to the mountains, check the schedule online",
            "Flight to the conference is confirmed, remember to print boarding pass and pack laptop charger",
            "Car maintenance is due next month, need to schedule oil change and tire rotation",

            // Personal/Family (3 entries)
            "Call mom this weekend to catch up and discuss summer vacation plans with the family",
            "Birthday party planning for Sarah next month, need to book venue and send invitations",
            "Family vacation ideas: beach resort or mountain cabin, need to decide by end of week",

            // Learning/Hobbies (3 entries)
            "Learn guitar basics this month, found a good online tutorial series for beginners",
            "Photography workshop signup deadline is tomorrow, looks like a great opportunity to improve skills",
            "Read the book recommendations from the podcast, especially the one about productivity and habits"
        )

        // Realistic duration ranges in milliseconds (30-120 seconds)
        private val DURATION_RANGE = 30000L..120000L
    }

    /**
     * Generate 20 demo entries with realistic content and durations
     * Each entry goes through the full pipeline: save → embedding generation → database
     *
     * @return Flow<Int> Progress count (0-20)
     */
    fun execute(): Flow<Int> = flow {
        try {
            Log.d(TAG, "Starting demo entries generation...")
            Log.d(TAG, "Will generate ${DEMO_ENTRIES.size} entries")
            emit(0)

            DEMO_ENTRIES.forEachIndexed { index, text ->
                try {
                    // Generate realistic duration
                    val duration = Random.nextLong(DURATION_RANGE.first, DURATION_RANGE.last)

                    Log.d(TAG, "[${index + 1}/20] Starting generation for: \"${text.take(50)}...\"")
                    Log.d(TAG, "[${index + 1}/20] Duration: ${duration}ms (${duration/1000}s)")

                    // Use existing SaveVoiceMemoUseCase to ensure full pipeline
                    Log.d(TAG, "[${index + 1}/20] Calling SaveVoiceMemoUseCase...")

                    saveVoiceMemoUseCase.execute(text, duration)
                        .collect { memoId ->
                            Log.d(TAG, "[${index + 1}/20] Demo entry saved with ID: $memoId")
                            Log.d(TAG, "[${index + 1}/20] Progress: ${index + 1}/20 entries completed")
                        }

                    // Emit progress
                    emit(index + 1)
                    Log.d(TAG, "[${index + 1}/20] Progress emitted to UI")

                    // Small delay to make generation feel natural and avoid overwhelming the system
                    Log.d(TAG, "[${index + 1}/20] Waiting 100ms before next entry...")
                    delay(100)

                } catch (e: Exception) {
                    Log.e(TAG, "Error generating demo entry ${index + 1}: ${e.message}", e)
                    Log.e(TAG, "Entry text was: \"$text\"")
                    // Continue with next entry even if one fails
                }
            }

            Log.d(TAG, "Demo entries generation completed successfully!")
            Log.d(TAG, "Final stats: ${DEMO_ENTRIES.size} entries processed")

        } catch (e: Exception) {
            Log.e(TAG, "Fatal error during demo generation: ${e.message}", e)
            Log.e(TAG, "Stack trace:", e)
            throw e
        }
    }
}