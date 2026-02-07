/* Inventory workflow sample for navigation and structure view. */

function LoadInventory()
   local items := { "Mouse", "Keyboard", "Monitor" }
   local count := 0

   while count < Len(items)
      ? "Loaded: " + items[count + 1]
      count := count + 1
   enddo

   return items
endfunction

function FindItem(items, query)
   local index := 1
   while index <= Len(items)
      if items[index] == query
         return index
      endif
      index := index + 1
   enddo
   return 0
endfunction

procedure InventoryReport()
   local data := LoadInventory()
   local target := "Keyboard"
   local position := FindItem(data, target)

   if position == 0
      ? "Item not found: " + target
   else
      ? "Found " + target + " at position " + LTrim(Str(position))
   endif
return
